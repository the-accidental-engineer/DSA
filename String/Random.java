package com.tsys.loyalty.parser.travel.util;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsys.common.util.DateUtil;
import com.tsys.common.util.NumberUtil;
import com.tsys.loyalty.constants.AirEmailConstants;
import com.tsys.loyalty.constants.HotelConstants;
import com.tsys.loyalty.domain.payment.FulFillmentPaymentCard;
import com.tsys.loyalty.domain.travel.hotel.HotelInfo;
import com.tsys.loyalty.domain.travel.hotel.HotelReservationStatusCode;
import com.tsys.loyalty.domain.travel.hotel.HotelSearchPreferences;
import com.tsys.loyalty.domain.travel.hotel.HotelSmokingPreferenceType;
import com.tsys.loyalty.domain.travel.hotel.ImageDetails;
import com.tsys.loyalty.domain.travel.hotel.ImageInfo;
import com.tsys.loyalty.domain.travel.hotel.RoomDetails;
import com.tsys.loyalty.domain.travel.hotel.TextInfo;
import com.tsys.loyalty.domain.travel.hotel.expedia.Address;
import com.tsys.loyalty.domain.travel.hotel.expedia.Amount;
import com.tsys.loyalty.domain.travel.hotel.expedia.CancelPenalties;
import com.tsys.loyalty.domain.travel.hotel.expedia.ExpediaProperty;
import com.tsys.loyalty.domain.travel.hotel.expedia.Fees;
import com.tsys.loyalty.domain.travel.hotel.expedia.OccupancyPricing;
import com.tsys.loyalty.domain.travel.hotel.expedia.Phone;
import com.tsys.loyalty.domain.travel.hotel.expedia.PropertyAvailability;
import com.tsys.loyalty.domain.travel.hotel.expedia.Rate;
import com.tsys.loyalty.domain.travel.hotel.expedia.Room;
import com.tsys.loyalty.domain.travel.hotel.expedia.booking.request.BillingContact;
import com.tsys.loyalty.domain.travel.hotel.expedia.booking.request.ExpediaBookingRequest;
import com.tsys.loyalty.domain.travel.hotel.expedia.booking.request.Payment;
import com.tsys.loyalty.domain.travel.hotel.expedia.booking.request.RoomBooking;
import com.tsys.loyalty.domain.travel.hotel.expedia.itinerary.ItineraryResponse;
import com.tsys.loyalty.domain.travel.hotel.expedia.property.Property;
import com.tsys.loyalty.domain.travel.hotel.expedia.property.RoomContainer;
import com.tsys.loyalty.domain.travel.hotel.expedia.tripAdvisory.TripAdvisory;
import com.tsys.loyalty.domain.types.RatePeriodType;

public class HotelEntityHelper {
	private static final String CANCEL_PENALTIES_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String NOTREFUNDABLE_CANCELLATION_POLICY = "This rate is non-refundable. If you choose to change or cancel this booking you will not be refunded any of the payment.";
	private static final String HOTEL_CANCELLATION_POLICY = "We understand that sometimes your travel plans change. We do not charge a change or cancel fee. However, this property ( %s ) imposes the following penalty to its customers that we are required to pass on: ";
	private static final String HOTEL_ROOM_CANCELLATION = "Cancellations or changes made after %s will result in %s as penalty. ";
	private static final String HOTEL_BOOKING_VALUE_AS_PENALTY = "%s of the booking value";
	private static final String HOTEL_NIGHT_AS_PENALTY = "%s night’s room rate plus tax";
	private static final String HOTEL_AMOUNT_AS_PENALTY = "%s %s";
	private static final String SLASH = " / ";
	public static final String MEMBERS = "Members";
	public static final String MEMBER = "Member";
	public static final String PATTREN = "[^ a-zA-Z0-9%]";
	public static final String OFF = "off";
	public static final String PRICE_FRISTLETTER_UP = "Price";
	public static final String PRICE_LOWER = "price";
	
	private static final Logger logger = LoggerFactory.getLogger(HttpJiraClient.class);
	
	public static HotelInfo processExpediaItineraryResult(ItineraryResponse itinerary) throws Exception {
		HotelInfo hotelInfo = new HotelInfo();
		setItineraryIdSelectedRoomsAndCnfNumber(itinerary, hotelInfo);
		setCancelLinks(itinerary, hotelInfo);
		if(nonNull(itinerary.getRooms()) && nonNull(itinerary.getRooms().get(0)) 
				&& nonNull(itinerary.getRooms().get(0).getRate()) 
				&& nonNull(itinerary.getRooms().get(0).getRate().getCancelPenalties())) {
			List<CancelPenalties> cancelPenalties= itinerary.getRooms().get(0).getRate().getCancelPenalties();
			if(StringUtils.isNotBlank(cancelPenalties.get(0).getStart())) {     
			   hotelInfo.setCancelPenaltyStartDate(cancelPenalties.get(0).getStart());
			}
			/*
			 * BOP-53732 
			 * We don't have to show the cancel button, in case the hotel checkIn date is in the past when compared to 
			 * currentTimestamp
			 */
			if(StringUtils.isNotBlank(itinerary.getRooms().get(0).getCheckIn())) {
			   hotelInfo.setCheckInDate(DateUtil.parseDate(itinerary.getRooms().get(0).getCheckIn()));
			}
			
		}
		return hotelInfo;
	}

	private static void setCancelLinks(ItineraryResponse itinerary, HotelInfo hotelInfo) {
		List<String> cancelLinks = new ArrayList<>();
		if(nonNull(itinerary.getLinks()) && nonNull(itinerary.getLinks().getCancel()) 
				&& StringUtils.isNotBlank(itinerary.getLinks().getCancel().getHref())) {
			cancelLinks.add(itinerary.getLinks().getCancel().getHref());
		} else if(nonNull(itinerary.getRooms())){
			itinerary.getRooms().forEach( room -> {
				if(nonNull(room.getLinks()) && nonNull(room.getLinks().getCancel()) && StringUtils.isNotBlank(room.getLinks().getCancel().getHref())) {
					cancelLinks.add(room.getLinks().getCancel().getHref());
				}
			});
		}
		if (CollectionUtils.isNotEmpty(cancelLinks)) {
			hotelInfo.setCancelLinks(cancelLinks);
		}
	}

	private static void setItineraryIdSelectedRoomsAndCnfNumber(ItineraryResponse itinerary, HotelInfo hotelInfo) {
		if(StringUtils.isNotBlank(itinerary.getItineraryId())) {
			hotelInfo.setItineraryId(itinerary.getItineraryId());
		}
		List<String> confirmationNumbers = new ArrayList<>();
		List<RoomDetails> selectedRooms = new ArrayList<>();
		if(nonNull(itinerary.getRooms())) {
			itinerary.getRooms().forEach( room -> {
				RoomDetails roomDetails = new RoomDetails();
				if(nonNull(room.getConfirmationId()) && StringUtils.isNotBlank(room.getConfirmationId().getExpedia())) {
					confirmationNumbers.add(room.getConfirmationId().getExpedia());
				}
				roomDetails.setReservationStatusCode(HotelConstants.RESERVATION_STATUS_BOOKED.equals(room.getStatus()) 
									? HotelReservationStatusCode.CF:(HotelConstants.RESERVATION_STATUS_CANCELED.equals(room.getStatus())) 
											? HotelReservationStatusCode.CX:HotelReservationStatusCode.DT);
				/**
				 * BOP-53732 
				 * In case the rate is non-refundable, and if you choose to change or cancel the booking you will not be refunded 
				 * any of the payment. 
				 * So, specifically for the non-refundable rates/room we DON'T have to show the cancel button on the AccountActivity
				 * page of lux UI
				 */
				if(nonNull(room.getRate()) && nonNull(room.getRate().isRefundable())) {
					roomDetails.setNonRefundable(!Boolean.valueOf(room.getRate().isRefundable()));
				}
				
				selectedRooms.add(roomDetails);
			});
		}
		if (CollectionUtils.isNotEmpty(selectedRooms)) {
			hotelInfo.setSelectedRooms(selectedRooms);
		}
		if(CollectionUtils.isNotEmpty(confirmationNumbers)) {
			hotelInfo.setConfirmationNumbers(confirmationNumbers);
		}
	}
	
	public static HotelInfo buildExpediaHotelInfo(HotelSearchPreferences searchPreferences, PropertyAvailability pa, 
			ExpediaProperty expProp, TripAdvisory tripAdv) throws RuntimeException {
		HotelInfo hotelInfo = new HotelInfo();
		setHotelBasicInfo(expProp, hotelInfo, searchPreferences);
		hotelInfo.setNumberOfRoomsNeeded(HotelConstants.ROOMS_NEEDED);
		setTripAdvisorDetails(tripAdv, hotelInfo);
		hotelInfo.setRatePeriod(RatePeriodType.DAILY.getDescription());
		hotelInfo.setRatePeriodType(RatePeriodType.DAILY);
		hotelInfo.setChainCode(HotelConstants.CHAIN_CODE);
		hotelInfo.setChainName(HotelConstants.CHAIN_NAME);
		Rate rate = setPropertyAvailablityContent(pa, hotelInfo);
		setAmountsBeforeTax(searchPreferences, pa, hotelInfo, rate);
		if (nonNull(pa.getLinks()) && nonNull(pa.getLinks().getAdditionalRates()) 
				&& StringUtils.isNotBlank(pa.getLinks().getAdditionalRates().getHref())) {
			String additionalRatesLink=pa.getLinks().getAdditionalRates().getHref();
			hotelInfo.setAdditionalRatesLink(additionalRatesLink);
		}
		if(nonNull(searchPreferences.getProgramItemApiKey())) {
			hotelInfo.setProgramItemApiKey(searchPreferences.getProgramItemApiKey());
			hotelInfo.setFenced(rate.isFencedDeal());
		}
		return hotelInfo;
	}

	private static void setAmountsBeforeTax(HotelSearchPreferences searchPreferences, PropertyAvailability pa, HotelInfo hotelInfo, Rate rate) {
		int numberOfRooms=0;
		hotelInfo.setNumberOfNights(searchPreferences.getNumberOfNights());
		Double totalExclusiveAmount = Double.valueOf(0);
		Double totalStrikeThroughAmount = Double.valueOf(0);
		for(Map.Entry<String,OccupancyPricing> pricingEntry: rate.getOccupancyPricing().entrySet()) {
			Double occupancyExclusiveAmount = Double.valueOf(0);
			OccupancyPricing pricing=pricingEntry.getValue();
			if (nonNull(pricing.getTotals()) && nonNull(pricing.getTotals().getExclusive()) && 
					nonNull(pricing.getTotals().getExclusive().getRequestCurrency()) && 
					StringUtils.isNotBlank(pricing.getTotals().getExclusive().getRequestCurrency().getValue())) {
				occupancyExclusiveAmount += Double.parseDouble(pricing.getTotals().getExclusive().getRequestCurrency().getValue());
			}
			if (nonNull(pricing.getTotals()) && nonNull(pricing.getTotals().getStrikeThrough()) && 
					nonNull(pricing.getTotals().getStrikeThrough().getRequestCurrency()) && 
					StringUtils.isNotBlank(pricing.getTotals().getStrikeThrough().getRequestCurrency().getValue())) {
				totalStrikeThroughAmount +=Double.parseDouble(pricing.getTotals().getStrikeThrough().getRequestCurrency().getValue());
			} 
			occupancyExclusiveAmount = calculateTotalExclusiveAmount(pa, occupancyExclusiveAmount, pricingEntry, pricing);
			if(MapUtils.isNotEmpty(searchPreferences.getOccupancies()) && 
				nonNull(searchPreferences.getOccupancies().get(pricingEntry.getKey()))) {
				numberOfRooms+=searchPreferences.getOccupancies().get(pricingEntry.getKey());
				occupancyExclusiveAmount*=searchPreferences.getOccupancies().get(pricingEntry.getKey());
			} else {
				numberOfRooms++;
			}
			totalExclusiveAmount+=occupancyExclusiveAmount;
		}
		setAmountsBeforeTax(hotelInfo, numberOfRooms, searchPreferences.getNumberOfNights(), totalExclusiveAmount, totalStrikeThroughAmount);
	}

	private static void setAmountsBeforeTax(HotelInfo hotelInfo, int numberOfRooms, 
			long numerOfNights, Double totalExclusiveAmount, Double totalStrikeThroughAmount) {
		if(numberOfRooms !=0) {
			if(totalExclusiveAmount!=0) {
				hotelInfo.setAmountBeforeTax((totalExclusiveAmount/numberOfRooms)/numerOfNights);
			}
			if(totalStrikeThroughAmount!=0) {
				hotelInfo.setBaseAmountBeforeTax((totalStrikeThroughAmount/numberOfRooms)/numerOfNights);
			} else {
				hotelInfo.setBaseAmountBeforeTax((totalExclusiveAmount/numberOfRooms)/numerOfNights);
			}
		}
	}

	private static Double calculateTotalExclusiveAmount(PropertyAvailability pa, Double occupancyExclusiveAmount,
			Map.Entry<String, OccupancyPricing> pricingEntry, OccupancyPricing pricing) {
		if(CollectionUtils.isNotEmpty(pricing.getNightly())) {
			List<List<Amount>> amounts=pricing.getNightly();
		    List<Optional<Amount>> amountList=amounts.stream()
		                                               .map(amtList-> amtList.stream()
		                                                           .filter(amt->HotelConstants.EXTRA_PERSON_FEE_KEY.equals(amt.getType()))
		                                                           .findFirst())
		                                               .collect(Collectors.toList());
		    Double totalExtraPersonFeeAmt= amountList.stream()
		            		                          	.filter(elm->elm.isPresent())
		            		                          	.mapToDouble(amt->Double.valueOf(amt.get().getValue()))
		            		                          	.sum();
	    	logger.debug(String.format("HotelId %s with Occupancy %s has Extra Person Fees %f:",pa.getPropertyId(),pricingEntry.getKey(),totalExtraPersonFeeAmt));
	    	logger.debug(String.format("HotelId %s with Occupancy %s has TotalExclusive Amount %f:",pa.getPropertyId(),pricingEntry.getKey(),occupancyExclusiveAmount));
	    	occupancyExclusiveAmount-=totalExtraPersonFeeAmt;
		}
		return occupancyExclusiveAmount;
	}

	private static Rate setPropertyAvailablityContent(PropertyAvailability pa, HotelInfo hotelInfo) {
		if (nonNull(pa.getPropertyId())) {
			hotelInfo.setExpediaPropertyId(Integer.valueOf(pa.getPropertyId()));
		}
		Rate rate= new Rate();
		if(nonNull(pa.getRooms()) && nonNull(pa.getRooms().get(0).getRates())) {
			 rate=pa.getRooms().get(0).getRates().get(0);
		}
		if(nonNull(rate.getPromotions()) && nonNull(rate.getPromotions().getDeal()) && StringUtils.isNotBlank(rate.getPromotions().getDeal().getDescription())) {
			String promoDescription= rate.getPromotions().getDeal().getDescription();
			if(promoDescription.contains(MEMBER)){
				// below replace method is using to replace "Member’s price: 10%" to  "Member Price 10% off"
				promoDescription = promoDescription.replaceAll(PATTREN, "").replace(MEMBERS, MEMBER).replace(PRICE_LOWER, PRICE_FRISTLETTER_UP) + " " + OFF;
			}
			hotelInfo.setPromoDescription(promoDescription);
		}
		if(nonNull(rate.getAvailableRooms())) {
			hotelInfo.setCurrentAllotment(rate.getAvailableRooms());
		}
		hotelInfo.setFreeCancellation(rate.isRefundable());
		return rate;
	}
	
	private static void setTripAdvisorDetails(TripAdvisory tripAdv, HotelInfo hotelInfo) {
		if(nonNull(tripAdv)) {
			if(nonNull(tripAdv.getRating())) {
				Double tripAdvisorRating = Double.valueOf(tripAdv.getRating());
				hotelInfo.setTripAdvisorRating(tripAdvisorRating);
			}
			if(nonNull(tripAdv.getCount())) {
				Integer tripAdvisorReviewcount = Integer.valueOf(tripAdv.getCount());
				hotelInfo.setTripAdvisorReviewCount(tripAdvisorReviewcount);
			}
			if (nonNull(tripAdv.getLinks()) && nonNull(tripAdv.getLinks().getRatingImage()) && nonNull(tripAdv.getLinks().getRatingImage().getHref())) {
				hotelInfo.setTripAdvisorImageUrl(tripAdv.getLinks().getRatingImage().getHref());
			}
		}
	}

	private static void setHotelBasicInfo(ExpediaProperty expProp, HotelInfo hotelInfo, HotelSearchPreferences searchPreferences) {
		hotelInfo.setLocale(searchPreferences.getLocaleParameter());
		hotelInfo.setCheckInDate(searchPreferences.getCheckInDate());
		hotelInfo.setCheckOutDate(searchPreferences.getCheckOutDate());
		hotelInfo.setNumberOfRoomsNeeded(searchPreferences.getRooms());
		hotelInfo.setRoomPreferences(searchPreferences.getRoomPreferences());
		if (nonNull(expProp)) {
			if (nonNull(expProp.getExpPropertyId())) {
				hotelInfo.setHotelCode(expProp.getExpPropertyId());
			}
			if (nonNull(expProp.getPropertyName())) {
				hotelInfo.setHotelName(expProp.getPropertyName());
			}
			if (nonNull(expProp.getImage())) {
				hotelInfo.setSingleImage(expProp.getImage());
			}
			if (nonNull(expProp.getLatitude())) {
				Double latitude = Double.valueOf(expProp.getLatitude());
				hotelInfo.setLatitude(latitude);
			}
			if (nonNull(expProp.getLongitude())) {
				Double longitude = Double.valueOf(expProp.getLongitude());
				hotelInfo.setLongitude(longitude);
			}
			if (StringUtils.isNotBlank(expProp.getRatings())) {
				String ratingWithSuffix = expProp.getRatings();
				hotelInfo.setStarRating(Double.valueOf(ratingWithSuffix.split(AirEmailConstants.SPACE_STRING)[0]));
			} else {
				hotelInfo.setStarRating(0.0);
			}
			if (nonNull(expProp.getCategoryType())) {
				hotelInfo.setPropertyCategoryTpe(expProp.getCategoryType());
			}
			if(StringUtils.isNotBlank(expProp.getChildFriendly())) {
				hotelInfo.setChildFriendly(expProp.getChildFriendly());
			}
			if (nonNull(expProp.getPhone())) {
				hotelInfo.setPhoneNumber(expProp.getPhone());
			}
		}
	}
	
	public static void processExpediaHotelDetails(Property prop, HotelInfo hotelInfo, PropertyAvailability roomAvailability) {
		if(nonNull(prop)) {
			setHotelLocation(hotelInfo, prop);
			if (nonNull(prop.getCheckin()) && nonNull(prop.getCheckin().getBeginTime())) {
				hotelInfo.setCheckInTime(prop.getCheckin().getBeginTime());
			}
			if (nonNull(prop.getCheckout()) && nonNull(prop.getCheckout().getTime())) {
				hotelInfo.setCheckOutTime(prop.getCheckout().getTime());
			}
			hotelInfo.setGeneralText(new ArrayList<TextInfo>());
			setPropertyTextInfo(prop, hotelInfo);
			if (nonNull(prop.getPolicies()) && nonNull(prop.getPolicies().getKnowBeforeYouGo())) {
				hotelInfo.setRoomInformation(prop.getPolicies().getKnowBeforeYouGo());
			}
			if (nonNull(prop.getFees()) && nonNull(prop.getFees().getOptional())) {
				hotelInfo.setRoomInformation(hotelInfo.getRoomInformation() + "<br>" + prop.getFees().getOptional());
			}
			if (nonNull(prop.getFees()) && nonNull(prop.getFees().getMandatory())) {
				hotelInfo.setMandatoryFeesDescription(prop.getFees().getMandatory());
			}
			setHotelsImages(prop, hotelInfo);
			setAllAmenities(prop, hotelInfo);
			processExpediaHotelRoomDetails(hotelInfo, roomAvailability, prop);
			Collections.sort(hotelInfo.getRooms(),(room1,room2)->room1.getAverageRateBeforeTax().compareTo(room2.getAverageRateBeforeTax()));
		}
	}

	private static void setAllAmenities(Property prop, HotelInfo hotelInfo) {
		if(nonNull(prop.getAmenities()) && MapUtils.isNotEmpty(prop.getAmenities().getAttributeContainer())) {
			List<TextInfo> amenityTextList = new ArrayList<>();
			hotelInfo.setAmenityText(amenityTextList);
			prop.getAmenities()
				.getAttributeContainer()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(k -> (String) k.getKey(), e -> (String) e.getValue().getName()))
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue())
				.forEach(aminityEntry ->{
					TextInfo textInfo = new TextInfo();
					textInfo.setText(aminityEntry.getValue());
					amenityTextList.add(textInfo);
				});
		}
	}

	private static void setHotelsImages(Property prop, HotelInfo hotelInfo) {
		if(CollectionUtils.isNotEmpty(prop.getImages())) {
			ImageDetails allGalleryImages = new ImageDetails();
			prop.getImages()
				.stream()
				.forEach(img ->{
					ImageInfo imageInfo = new ImageInfo();
					if(nonNull(img.getLinks().getImageContainer().get("350px"))) {
						imageInfo.setBaseUrl(img.getLinks().getImageContainer().get("350px").getHref());
					}else if(nonNull(img.getLinks().getImageContainer().get("70px"))) {
						imageInfo.setBaseUrl(img.getLinks().getImageContainer().get("70px").getHref());
					}else {
						if(MapUtils.isNotEmpty(img.getLinks().getImageContainer())) {
							imageInfo.setBaseUrl(img.getLinks()
												    .getImageContainer()
												    .entrySet()
												    .stream()
												    .findFirst()
												    .get()
												    .getValue()
												    .getHref()
												);
						}
					}
					imageInfo.setCaption(img.getCaption());
					allGalleryImages.addImageInfo(imageInfo);
				});
			hotelInfo.setAllGalleryImages(allGalleryImages);
		}
	}

	private static void setPropertyTextInfo(Property prop, HotelInfo hotelInfo) {
		if(nonNull(prop.getDescriptions())) {
			if (nonNull(prop.getDescriptions().getAttractions())) {
				TextInfo textInfo = new TextInfo();
				textInfo.setText(prop.getDescriptions().getAttractions());
				hotelInfo.getGeneralText().add(textInfo);
			}
			
			List<TextInfo> propertyTextList = new ArrayList<>();
			hotelInfo.setPropertyText(propertyTextList);
			List<TextInfo> accessibilityTextList = new ArrayList<>();
			hotelInfo.setAccessibilityText(accessibilityTextList);
			if (nonNull(prop.getDescriptions().getLocation())) {
				TextInfo textInfo = new TextInfo();
				textInfo.setText(StringEscapeUtils.unescapeHtml(prop.getDescriptions().getLocation()));
				hotelInfo.getPropertyText().add(textInfo);
				hotelInfo.getAccessibilityText().add(textInfo);
			}
			
			List<TextInfo> diningTextList = new ArrayList<>();
			hotelInfo.setDiningText(diningTextList);
			if (nonNull(prop.getDescriptions().getDining())) {
				TextInfo textInfo = new TextInfo();
				textInfo.setText(prop.getDescriptions().getDining());
				hotelInfo.getDiningText().add(textInfo);
			}
			
			List<TextInfo> corpLocationTextList = new ArrayList<>();
			hotelInfo.setCorpLocationText(corpLocationTextList);
			if (nonNull(prop.getDescriptions().getLocation())) {
				TextInfo textInfo = new TextInfo();
				textInfo.setText(prop.getDescriptions().getLocation());
				hotelInfo.getCorpLocationText().add(textInfo);
			}
			
			List<TextInfo> generalAmenityTextList = new ArrayList<>();
			hotelInfo.setGeneralAmenityText(generalAmenityTextList);
			if (nonNull(prop.getDescriptions().getAmenities())) {
				TextInfo textInfo = new TextInfo();
				textInfo.setText(prop.getDescriptions().getAmenities());
				hotelInfo.getGeneralAmenityText().add(textInfo);
			}
			
			List<TextInfo> businessTextList = new ArrayList<>();
			hotelInfo.setBusinessText(businessTextList);
			if (nonNull(prop.getDescriptions().getBusinessAmenities())) {
				TextInfo textInfo = new TextInfo();
				textInfo.setText(prop.getDescriptions().getBusinessAmenities());
				hotelInfo.getBusinessText().add(textInfo);
			}
		}
	}
	
	private static void setHotelLocation(HotelInfo hotelInfo, Property prop) {
		if (nonNull(prop.getAddress())) {
			if (nonNull(prop.getAddress().getLineOne())) {
				hotelInfo.getAddressType().setAddressLine1(prop.getAddress().getLineOne());
			}
			if (nonNull(prop.getAddress().getLineTwo())) {
				hotelInfo.getAddressType().setAddressLine2(prop.getAddress().getLineTwo());
			}
			if (nonNull(prop.getAddress().getCity())) {
				hotelInfo.getAddressType().setCity(prop.getAddress().getCity());
			}
			if (nonNull(prop.getAddress().getState_province_code())) {
				hotelInfo.getAddressType().setStateProvince(prop.getAddress().getState_province_code());
			}
			if (nonNull(prop.getAddress().getPostalCode())) {
				hotelInfo.getAddressType().setPostalZip(prop.getAddress().getPostalCode());
			}
			if (nonNull(prop.getAddress().getCountryCode())) {
				hotelInfo.getAddressType().setCountry(prop.getAddress().getCountryCode());
			}
		}
	}
	
	
	/**
	 * 
	 * Setting All Room Detail Info.
	 * @param hotelInfo
	 * @param roomAvailability
	 * @param prop
	 */
	private static void processExpediaHotelRoomDetails(HotelInfo hotelInfo, PropertyAvailability roomAvailability, Property prop) {
		setCheckinInstructions(hotelInfo, prop);
		
		if(nonNull(prop.getRooms()) 
				&& MapUtils.isNotEmpty(prop.getRooms().getRoomContainer()) 
				&& CollectionUtils.isNotEmpty(roomAvailability.getRooms())) {
			List<RoomDetails> rooms = new ArrayList<>();
			hotelInfo.setRooms(rooms);
			AtomicInteger i = new AtomicInteger(1);
			prop.getRooms().getRoomContainer() .entrySet() .forEach(roomEntry -> {
				if(roomAvailability.getRooms().stream().anyMatch(room -> room.getRoomId().equals(roomEntry.getKey()))) {
					RoomContainer entryRoom = roomEntry.getValue();
					Map<String, String> bedTypeMap = new HashMap<>();
					fillRoomDetails(hotelInfo, roomAvailability, rooms, i, roomEntry, entryRoom, bedTypeMap);
				}
			});
		}
	}

	private static void fillRoomDetails(HotelInfo hotelInfo, PropertyAvailability roomAvailability,
			List<RoomDetails> rooms, AtomicInteger i, Entry<String, RoomContainer> roomEntry, RoomContainer entryRoom,
			Map<String, String> bedTypeMap) {
		roomAvailability.getRooms()
						.stream()
						.filter(room -> room.getRoomId().equals(roomEntry.getKey()))
						.forEach(room -> {
							room.getRates().forEach(rate -> {
								RoomDetails roomDetails = new RoomDetails();
								rooms.add(roomDetails);
								roomDetails.setId(i.getAndIncrement());
								setBedTypesInRoomDetails(bedTypeMap, rate, roomDetails);
								if(nonNull(rate.getRateLinks()) && nonNull(rate.getRateLinks().getPaymentOptions())) {
									roomDetails.setPaymentOptionsLink(rate.getRateLinks().getPaymentOptions().getHref());
								}
								setMaxOccupancyAndRoomsDesc(rate.getId(), entryRoom, roomDetails, room);
								setRoomImageUrls(entryRoom, roomDetails);
								setSmokingPreferences(entryRoom, roomDetails);
								setServiceTaxAndSalesTaxAmount(hotelInfo, rate, roomDetails);
								Double roomTotalRateAfterTax = Double.valueOf(0);
								Double roomTotalRateBeforeTax = Double.valueOf(0);
								Double strikeThroughRate = Double.valueOf(0);
								List<Double> extraPersonFeeList=new ArrayList<>();
								if(MapUtils.isNotEmpty(rate.getOccupancyPricing()) ) {
									Map<String,Double> inclusiveCurrencyMap=new HashMap<>();
									Map<String,Double> exclusiveCurrencyMap=new HashMap<>();
									Map<String,Double> strikeThroughCurrencyMap=new HashMap<>();
									Map<String,Double> extraPersonFeeMap=new HashMap<>();
									List<Double> inclusiveCurrencyAmountList=new ArrayList<>();
									List<Double> exclusiveCurrencyAmountList=new ArrayList<>();
									List<Double> strikeThroughCurrencyAmountList=new ArrayList<>();
									fillStrikeThroughCurrencyMap(rate, inclusiveCurrencyMap, exclusiveCurrencyMap, strikeThroughCurrencyMap, extraPersonFeeMap);
									fillCurrencyAmountList(hotelInfo, extraPersonFeeList, inclusiveCurrencyMap,
											exclusiveCurrencyMap, strikeThroughCurrencyMap, extraPersonFeeMap,
											inclusiveCurrencyAmountList, exclusiveCurrencyAmountList,
											strikeThroughCurrencyAmountList);
								   roomDetails.setExclusiveCurrencyAmountPerOccupancyMap(exclusiveCurrencyMap);
								   int totalNoOfRooms=hotelInfo.getOccupancies().values().stream().mapToInt(Integer::valueOf).sum();
								   roomTotalRateAfterTax=inclusiveCurrencyAmountList.stream().mapToDouble(Double::doubleValue).sum();
								   roomTotalRateAfterTax=NumberUtil.round(roomTotalRateAfterTax, 2);
								   roomTotalRateBeforeTax=exclusiveCurrencyAmountList.stream().mapToDouble(Double::doubleValue).sum();
								   roomTotalRateBeforeTax=roomTotalRateBeforeTax/totalNoOfRooms;
								   roomTotalRateBeforeTax=NumberUtil.round(roomTotalRateBeforeTax, 2);
								   strikeThroughRate=strikeThroughCurrencyAmountList.stream().mapToDouble(Double::doubleValue).sum();
								   strikeThroughRate=strikeThroughRate/totalNoOfRooms;
								   strikeThroughRate=NumberUtil.round(strikeThroughRate, 2);
								}
								setAverageRatesAfterTax(hotelInfo, roomDetails, roomTotalRateAfterTax, roomTotalRateBeforeTax, strikeThroughRate);
								roomDetails.setTotalRateAfterTax(roomTotalRateAfterTax);
								setSurcharges(roomDetails, extraPersonFeeList);
								setCancellationPolicy(hotelInfo, rate, roomDetails);
								setHotelFeeInRoomDetails(rate, roomDetails);
				});
			});
	}

	private static void setAverageRatesAfterTax(HotelInfo hotelInfo, RoomDetails roomDetails, 
			Double roomTotalRateAfterTax, Double roomTotalRateBeforeTax, Double strikeThroughRate) {
		Long totalNights = hotelInfo.getNumberOfNights();
		Double roomTotalRateAfterTaxPerNight=NumberUtil.round((roomTotalRateAfterTax/totalNights),2);
		Double roomTotalRateBeforeTaxPerNight= NumberUtil.round((roomTotalRateBeforeTax/totalNights),2);
		Double strikeThroughRatePerNight= NumberUtil.round((strikeThroughRate/totalNights),2);
		roomDetails.setAverageRateAfterTax(roomTotalRateAfterTaxPerNight);
		roomDetails.setAverageRateBeforeTax(roomTotalRateBeforeTaxPerNight);
		if(!strikeThroughRate.equals(Double.valueOf(0))) {
			roomDetails.setAverageBaseRateAfterTax(strikeThroughRatePerNight);
			roomDetails.setAverageBaseRateBeforeTax(strikeThroughRatePerNight);
		} else {
			roomDetails.setAverageBaseRateAfterTax(roomTotalRateAfterTaxPerNight);
			roomDetails.setAverageBaseRateBeforeTax(roomTotalRateBeforeTaxPerNight);
		}
	}

	private static void setSurcharges(RoomDetails roomDetails, List<Double> extraPersonFeeList) {
		if(CollectionUtils.isNotEmpty(extraPersonFeeList)) {
			Double totalExtraPersonFee=0.0d;
			for(Double fee:extraPersonFeeList) {	
				if(nonNull(fee)){																		
					totalExtraPersonFee+= fee;											
				} 
			}
			roomDetails.getSurcharges().put(HotelConstants.SURCHARGE_EXTRA_PERSON_FEE,totalExtraPersonFee);
		}
	}

	private static void setCancellationPolicy(HotelInfo hotelInfo, Rate rate, RoomDetails roomDetails) {
		if (CollectionUtils.isNotEmpty(rate.getCancelPenalties())) {
			logger.debug("SetCancellationPolicy --> Hotel Name - {}, Room Desc - {}, isRefundable - {}, Max Occupancy - {}, AverageBaseRateAfterTax - {}, List of cancel penalties - {}",hotelInfo.getHotelName(), roomDetails.getRoomDescKey(), rate.isRefundable(),roomDetails.getMaxOccupancy(), roomDetails.getAverageBaseRateAfterTax(),rate.getCancelPenalties().toString());
			String defaultCancelllationMsg = String.format(HOTEL_CANCELLATION_POLICY,hotelInfo.getHotelName());
			StringBuffer cancellationPenaltyMsg = new StringBuffer();
			rate.getCancelPenalties().forEach(cancelPenality -> {
				if (StringUtils.isNotBlank(cancelPenality.getStart())) {
					try {
						String startDate = StringUtils.EMPTY;
						StringBuffer paneltyMsg = new StringBuffer();
						LocalDateTime startDateObj = Instant.parse(cancelPenality.getStart()).atZone(ZoneId.systemDefault()).toLocalDateTime();
						startDate = startDateObj.format(DateTimeFormatter.ofPattern(CANCEL_PENALTIES_DATE_FORMAT));
						if (StringUtils.isNotBlank(cancelPenality.getPercent())) {
							paneltyMsg.append(String.format(HOTEL_BOOKING_VALUE_AS_PENALTY, cancelPenality.getPercent()));
						}
						if (StringUtils.isNotBlank(cancelPenality.getNights())) {
							if (!paneltyMsg.toString().isEmpty()) {
								paneltyMsg.append((SLASH));
							}
							paneltyMsg.append(String.format(HOTEL_NIGHT_AS_PENALTY, cancelPenality.getNights()));
						}						 
						if (StringUtils.isNotBlank(cancelPenality.getAmount())) {
							if (!paneltyMsg.toString().isEmpty()) {
								paneltyMsg.append((SLASH));
							}
							paneltyMsg.append(String.format(HOTEL_AMOUNT_AS_PENALTY, cancelPenality.getAmount(), cancelPenality.getCurrency()));
						}
						if (!paneltyMsg.toString().isEmpty()) {
							cancellationPenaltyMsg.append(String.format(HOTEL_ROOM_CANCELLATION, startDate, paneltyMsg));
						}
					} catch (Exception e) {
						logger.error("<<Cancel Penalties Invalid Data Error -->>", e);
					} 
					
				}
			});
			roomDetails.setCancellationPolicy(!cancellationPenaltyMsg.toString().isEmpty() ? defaultCancelllationMsg + cancellationPenaltyMsg.toString(): StringUtils.EMPTY);
		}
		if (nonNull(rate.isRefundable())) {
			roomDetails.setNonRefundable(!Boolean.valueOf(rate.isRefundable()));
			if (roomDetails.isNonRefundable()) {
				roomDetails.setCancellationPolicy(NOTREFUNDABLE_CANCELLATION_POLICY);
			}
		}
	}

	private static void setHotelFeeInRoomDetails(Rate rate, RoomDetails roomDetails) {
		Fees  hotelFees = rate.getOccupancyPricing().entrySet().stream().findFirst().get().getValue().getFees();
		if(nonNull(hotelFees)) {
			if(nonNull(hotelFees.getMandatoryFee()) && nonNull(hotelFees.getMandatoryFee().getRequestCurrency())) {
				roomDetails.getHotelFees().put("MandatoryFee", Double.valueOf(hotelFees.getMandatoryFee().getRequestCurrency().getValue()));
			}
			if(nonNull(hotelFees.getMandatoryTax()) && nonNull(hotelFees.getMandatoryTax().getRequestCurrency())) {
				roomDetails.getHotelFees().put("MandatoryTax", Double.valueOf(hotelFees.getMandatoryTax().getRequestCurrency().getValue()));
			}
			if(nonNull(hotelFees.getResortFee()) && nonNull(hotelFees.getResortFee().getRequestCurrency())) {
				roomDetails.getHotelFees().put("ResortFee", Double.valueOf(hotelFees.getResortFee().getRequestCurrency().getValue()));
			}
		}
	}

	private static void setMaxOccupancyAndRoomsDesc(String rateId, RoomContainer entryRoom, RoomDetails roomDetails, Room room) {
		roomDetails.setControlNumber(rateId);
		roomDetails.setControlType(room.getRoomId());
		roomDetails.setRoomDescKey(room.getRoomName());
		roomDetails.setBedDescKey(room.getRoomName());
		List<String> roomTypeDescriptionList = new ArrayList<>();
		roomTypeDescriptionList.add(room.getRoomName());
		roomDetails.setRoomDescription(roomTypeDescriptionList);
		roomDetails.setLongDescription(entryRoom.getDescriptions().getOverview());
		if (nonNull(entryRoom.getOccupancy()) && nonNull(entryRoom.getOccupancy().getMaxAllowed())) {
			String maxOccupancy = entryRoom.getOccupancy().getMaxAllowed().getTotal();
			if (StringUtils.isNumeric(maxOccupancy)) {
				roomDetails.setMaxOccupancy(Integer.valueOf(maxOccupancy));
			}
		}
	}

	private static void setRoomImageUrls(RoomContainer entryRoom, RoomDetails roomDetails) {
		List<String> roomImageUrls = new ArrayList<>();
		if(nonNull(entryRoom.getImages())) { 
			entryRoom.getImages().forEach(img -> {
				if(nonNull( img.getLinks()) && MapUtils.isNotEmpty(img.getLinks().getImageContainer())) {
					if(nonNull(img.getLinks().getImageContainer().get("200px")) 
							&& nonNull(img.getLinks().getImageContainer().get("200px").getHref())) {
						roomImageUrls.add(img.getLinks().getImageContainer().get("200px").getHref());
					} else if(nonNull(img.getLinks().getImageContainer().get("350px"))
							&& nonNull(img.getLinks().getImageContainer().get("350px").getHref())) {
						roomImageUrls.add(img.getLinks().getImageContainer().get("350px").getHref());
					} else {
						roomImageUrls.add( img.getLinks().
								getImageContainer().entrySet().stream().findFirst().get().getValue() .getHref());
					}
				}
			});
			roomDetails.setRoomImageUrls(roomImageUrls);
		}
	}

	private static void fillCurrencyAmountList(HotelInfo hotelInfo, List<Double> extraPersonFeeList,
			Map<String, Double> inclusiveCurrencyMap, Map<String, Double> exclusiveCurrencyMap,
			Map<String, Double> strikeThroughCurrencyMap, Map<String, Double> extraPersonFeeMap,
			List<Double> inclusiveCurrencyAmountList, List<Double> exclusiveCurrencyAmountList,
			List<Double> strikeThroughCurrencyAmountList) {
		hotelInfo.getOccupancies().entrySet().stream().forEach(occupancy -> {
			String occupancyKey=occupancy.getKey();
			int noOfRooms=occupancy.getValue().intValue();
			if(MapUtils.isNotEmpty(inclusiveCurrencyMap)) {
				   Double inclusiveCurrencySum=inclusiveCurrencyMap.get(occupancyKey);
				   inclusiveCurrencyAmountList.add(inclusiveCurrencySum*noOfRooms);
			}
			if(MapUtils.isNotEmpty(extraPersonFeeMap)) {
				   Double extraPersonFeeAmount=extraPersonFeeMap.get(occupancyKey);
				   if(nonNull(extraPersonFeeAmount) &&  extraPersonFeeAmount != 0 ) {
					   extraPersonFeeList.add(extraPersonFeeAmount*noOfRooms);
				   }
			}
			if(MapUtils.isNotEmpty(exclusiveCurrencyMap)) {
				   Double exclusiveCurrencyAmount=exclusiveCurrencyMap.get(occupancyKey);
				   exclusiveCurrencyAmountList.add(exclusiveCurrencyAmount*noOfRooms);
			}
			
			if(MapUtils.isNotEmpty(strikeThroughCurrencyMap)) {
				   Double strikeThroughCurrencyAmount=strikeThroughCurrencyMap.get(occupancyKey);
				   strikeThroughCurrencyAmountList.add(strikeThroughCurrencyAmount*noOfRooms);
			}
		});
	}

	private static void fillStrikeThroughCurrencyMap(Rate rate, Map<String, Double> inclusiveCurrencyMap,
			Map<String, Double> exclusiveCurrencyMap, Map<String, Double> strikeThroughCurrencyMap, 
			Map<String, Double> extraPersonFeeMap) {
		rate.getOccupancyPricing().entrySet()
									.stream()
									.forEach(entry-> {
										Double inclusiveCurrencyAmount=0.0d;
										if(nonNull(entry.getValue().getTotals()) && nonNull(entry.getValue().getTotals().getInclusive())) {
											inclusiveCurrencyAmount=Double.valueOf(entry.getValue().getTotals().getInclusive().getRequestCurrency().getValue());
										} 
										inclusiveCurrencyMap.put(entry.getKey(),inclusiveCurrencyAmount);
										fillExclusiveCurrencyMap(exclusiveCurrencyMap, extraPersonFeeMap, entry);
										Double strikeThroughCurrencyAmount=0.0d;
										if(nonNull(entry.getValue().getTotals()) && nonNull(entry.getValue().getTotals().getStrikeThrough())){
											strikeThroughCurrencyAmount= Double.valueOf(entry.getValue().getTotals().getStrikeThrough().getRequestCurrency().getValue());
									   } 
										strikeThroughCurrencyMap.put(entry.getKey(),strikeThroughCurrencyAmount);
									});
	}

	private static void fillExclusiveCurrencyMap(Map<String, Double> exclusiveCurrencyMap,
			Map<String, Double> extraPersonFeeMap, Entry<String, OccupancyPricing> entry) {
		Double exclusiveCurrencyAmount=0.0d;
		List<Optional<Amount>> extraPersonAmountList  = entry.getValue().getNightly()
																		.stream()
																		.map(amtList-> {
																			return  amtList
																					.stream()
																					.filter(amt->"extra_person_fee".equals(amt.getType()))
																					.findFirst();
																		}).collect(Collectors.toList());
		Double sumOfExtraPersonFeeForAllNights = extraPersonAmountList
													.stream()
													.filter(elm->elm.isPresent())
													.mapToDouble(amt->Double.valueOf(amt.get().getValue()))
													.sum();
		if(nonNull(entry.getValue().getTotals()) && nonNull(entry.getValue().getTotals().getExclusive())){
				 exclusiveCurrencyAmount = Double.valueOf(entry.getValue().getTotals().getExclusive().getRequestCurrency().getValue());
		 } 
		if(sumOfExtraPersonFeeForAllNights!=0) {
			extraPersonFeeMap.put(entry.getKey(), sumOfExtraPersonFeeForAllNights);
			exclusiveCurrencyAmount=exclusiveCurrencyAmount-sumOfExtraPersonFeeForAllNights;
		 }
		exclusiveCurrencyMap.put(entry.getKey(),exclusiveCurrencyAmount);
	}

	private static void setServiceTaxAndSalesTaxAmount(HotelInfo hotelInfo, Rate rate, RoomDetails roomDetails) {
		Map<String,Double> serviceTaxMap = new HashMap<>();
		Map<String,Double> salesTaxMap = new HashMap<>();
		Double serviceTaxAmount = Double.valueOf(0);
		Double salesTaxAmount = Double.valueOf(0);
		if(nonNull(rate.getOccupancyPricing().entrySet().stream().findFirst().get().getValue().getNightly())) {
			fillSalesAndServiceTaxMap(rate, serviceTaxMap, salesTaxMap);
			List<Double> serviceTaxAmountList=new ArrayList<>();
			List<Double> salesTaxAmountList=new ArrayList<>();
			hotelInfo.getOccupancies()
					.entrySet()
						.stream()
							.forEach(occupancy -> {
								fillSalesTaxAmountList(serviceTaxMap, salesTaxMap, serviceTaxAmountList, salesTaxAmountList, occupancy);
							});
			serviceTaxAmount=serviceTaxAmountList.stream().mapToDouble(Double::doubleValue).sum();
			salesTaxAmount=salesTaxAmountList.stream().mapToDouble(Double::doubleValue).sum();
		}
		setSurcharges(roomDetails, serviceTaxAmount, salesTaxAmount);
	}

	private static void setSurcharges(RoomDetails roomDetails, Double serviceTaxAmount, Double salesTaxAmount) {
		roomDetails.setTotalSurcharges(serviceTaxAmount);
		if(serviceTaxAmount != 0) {
			roomDetails.getSurcharges().put(HotelConstants.TAX_AND_SERVICE_FEE,serviceTaxAmount);
			if(nonNull(salesTaxAmount) && salesTaxAmount!=0) {
				roomDetails.getSurcharges().put(HotelConstants.SERCHARGE_SALESTAX,salesTaxAmount);
			}
		} else if(salesTaxAmount != 0) {
			roomDetails.getSurcharges().put(HotelConstants.SERCHARGE_SALESTAX,salesTaxAmount);
		}
	}

	private static void fillSalesTaxAmountList(Map<String, Double> serviceTaxMap, Map<String, Double> salesTaxMap,
			List<Double> serviceTaxAmountList, List<Double> salesTaxAmountList, Entry<String, Integer> occupancy) {
		String occupancyKey=occupancy.getKey();
		int noOfRooms=occupancy.getValue().intValue();
		if(nonNull(serviceTaxMap)) {
			Double serviceTaxSum=serviceTaxMap.get(occupancyKey);
			serviceTaxAmountList.add(serviceTaxSum*noOfRooms);
		}	
		if(nonNull(salesTaxMap)) {
			Double salesTaxSum=salesTaxMap.get(occupancyKey);
			salesTaxAmountList.add(salesTaxSum*noOfRooms);
		}
	}

	private static void fillSalesAndServiceTaxMap(Rate rate, Map<String, Double> serviceTaxMap, Map<String, Double> salesTaxMap) {
		rate.getOccupancyPricing()
				.entrySet()
					.stream().forEach(entry-> {
								 List<List<Amount>> amounts=entry.getValue().getNightly();
								 List<Optional<Amount>> serviceTaxAmountList  = amounts.stream()
							    														.map(amtList -> {
							    															return  amtList
							    																		.stream()
							    																			.filter(amt -> 
							    																				HotelConstants.TAX_AND_SERVICE_FEE_KEY.equals(amt.getType())).findFirst();})
							    																				.collect(Collectors.toList());
							    Double sumOfServiceTaxForAllNights=serviceTaxAmountList.stream()
							    															.filter(elm->elm.isPresent())
							    																.mapToDouble(amt->Double.valueOf(amt.get().getValue()))
							    																	.sum();
							    serviceTaxMap.put(entry.getKey(),sumOfServiceTaxForAllNights);
							    List<Optional<Amount>> salesTaxAmountList = amounts.stream()
							    														.map(amtList-> { 
							    															return   amtList.stream()
							    																				.filter(amt->HotelConstants.SALE_TAX_KEY.equals(amt.getType()))
							    																					.findFirst();
							    														}).collect(Collectors.toList());
							    Double sumOfSalesTaxForAllNights=salesTaxAmountList.stream()
							    														.filter(elm->elm.isPresent())
							    															.mapToDouble(amt->Double.valueOf(amt.get().getValue()))
							    																.sum();
							                                   
							    salesTaxMap.put(entry.getKey(),sumOfSalesTaxForAllNights);
					});
	}

	private static void setSmokingPreferences(RoomContainer entryRoom, RoomDetails roomDetails) {
		if(nonNull(entryRoom.getAmenities()) && MapUtils.isNotEmpty(entryRoom.getAmenities().getAttributeContainer())) {
			List<String> roomValueAdds = new ArrayList<>();
			entryRoom.getAmenities().getAttributeContainer().entrySet().forEach(amentEntry -> {
				roomValueAdds.add(amentEntry.getValue().getName());
			});
			List<HotelSmokingPreferenceType> roomSmokingPref = new ArrayList<>();
			entryRoom.getAmenities().getAttributeContainer().entrySet().forEach(amenity -> {
				if(amenity.getValue().getName().equalsIgnoreCase(HotelConstants.NON_SMOKING)) {
					roomSmokingPref.add(HotelSmokingPreferenceType.NS);
				} else if(amenity.getValue().getName().equalsIgnoreCase(HotelConstants.SMOKING)){
					roomSmokingPref.add(HotelSmokingPreferenceType.S);
				} else if(amenity.getValue().getName().equalsIgnoreCase(HotelConstants.SMOKING_AND_NON_SMOKING)){
					roomSmokingPref.add(HotelSmokingPreferenceType.S);
					roomSmokingPref.add(HotelSmokingPreferenceType.NS);
				}
			});
			roomDetails.setSmokingPreferences(roomSmokingPref); 
		}
	}

	private static void setBedTypesInRoomDetails(Map<String, String> bedTypeMap, Rate rate, RoomDetails roomDetails) {
		if(nonNull(rate.getBedGroups()) && MapUtils.isNotEmpty(rate.getBedGroups().getBedContainer())){
			rate.getBedGroups().getBedContainer()
							   .entrySet()
							   .forEach(entry ->{
								   bedTypeMap.put(entry.getValue().getId(), entry.getValue().getDescription());
								   if(nonNull(entry.getValue().getLinks()) && nonNull(entry.getValue().getLinks().getPriceCheck())) {
									   roomDetails.setPriceCheckLink(entry.getValue().getLinks().getPriceCheck().getHref());
								   }
							   });
		}
		roomDetails.setBedTypes(bedTypeMap);
	}

	private static void setCheckinInstructions(HotelInfo hotelInfo, Property prop) {
		if(nonNull(prop.getCheckin())) {
			StringBuilder chekinInstns = new StringBuilder();
			if(nonNull(prop.getPolicies()) && nonNull(prop.getPolicies().getKnowBeforeYouGo())) chekinInstns.append(HotelConstants.CHECKIN_POLICY_INSTRUCTION).append(prop.getPolicies().getKnowBeforeYouGo()).append("</p>");
			if(nonNull(prop.getFees())) {
				if(nonNull(prop.getFees().getOptional()))  chekinInstns.append(HotelConstants.FEE_OPTIONAL_INSTRUCTION).append(prop.getFees().getOptional()).append("</p>");
				if(nonNull(prop.getFees().getMandatory())) chekinInstns.append(HotelConstants.FEE_MANADATORY_INSTRUCTION).append(prop.getFees().getMandatory()).append("</p>");
			}
			
			if(nonNull(prop.getCheckin().getInstructions())) chekinInstns.append(HotelConstants.CHECKIN_INSTRUCTION).append(prop.getCheckin().getInstructions()).append("</p>");
					
			hotelInfo.setCheckInInstructions(chekinInstns.toString());
			
			if(nonNull(prop.getCheckin().getSpecialInstructions())) {
				StringBuilder specialInstructions = new StringBuilder(prop.getCheckin().getSpecialInstructions());

				//This is returing nested object
				if(!prop.getAdditionalProperties().isEmpty()) {
					Optional.ofNullable((HashMap<String, Object>)prop.getAdditionalProperties().get(HotelConstants.VACATION_RENTAL_DETAILS))
			                .map(rentalDetails -> (HashMap<String, Object>)rentalDetails.get(HotelConstants.ENHANCED_HOUSE_RULES))
			                .map(houseRules -> (HashMap<String, Object>)houseRules.get(HotelConstants.MIN_BOOKING_AGE))
			                .map(minBookingAge -> (String)minBookingAge.get(HotelConstants.RULE))
			                .map(rule -> specialInstructions.append("<br/>").append(rule))
			                .orElse(null);
				}
				hotelInfo.setSpecialCheckInInstructions(specialInstructions.toString());
			}
		}
	}
	
	public static void setRoombookingDetails(HotelInfo hotelInfo, ExpediaBookingRequest expediaBookingRequest, Phone phone) {
		List<RoomBooking> rooms = new ArrayList<>();
		for (RoomDetails roomDetails : hotelInfo.getSelectedRooms()) {
			RoomBooking room = new RoomBooking();
			if (nonNull(hotelInfo.getContactInfo())) {
				room.setEmail(hotelInfo.getContactInfo().get(0).getEmailAddress());
			}
			room.setFamilyName(roomDetails.getUser().getLastName());
			room.setGivenName(roomDetails.getUser().getFirstName());
			room.setPhone(phone);
			if(Objects.nonNull(roomDetails.getSelectedSmokingPreference()) && StringUtils.isNotBlank(roomDetails.getSelectedSmokingPreference().getDescription()) && roomDetails.getSelectedSmokingPreference().getDescription().equals("smoking")) {
				room.setSmoking(Boolean.TRUE);
			}else {
				room.setSmoking(Boolean.FALSE);
			}
			rooms.add(room);
		}
		expediaBookingRequest.setRooms(rooms);
	}
	
	public static Phone setBillingContact(HotelInfo hotelInfo, FulFillmentPaymentCard card, BillingContact billingContact) {
		if(Objects.nonNull(card.getBillingAddress())) {
			Address address = new Address();
			address.setCity(card.getBillingAddress().getCity());
			address.setCountryCode(card.getBillingAddress().getCountry());
			address.setLine1(card.getBillingAddress().getAddressLine1());
			address.setLine2(card.getBillingAddress().getAddressLine2());
			address.setLine3(card.getBillingAddress().getAddressLine3());
			address.setPostalCode(card.getBillingAddress().getPostalZip());
			address.setStateProvinceCode(card.getBillingAddress().getStateProvince());
			billingContact.setAddress(address);
		}
		
		String firstName=card.getFullname().substring(0, card.getFullname().indexOf(HotelConstants.SPACE_SEPARATOR));
		String lastName=card.getFullname().substring(card.getFullname().lastIndexOf(HotelConstants.SPACE_SEPARATOR)+1, card.getFullname().length());
		billingContact.setFamilyName(lastName);
		billingContact.setGivenName(firstName);
		billingContact.setEmail(hotelInfo.getContactInfo().get(0).getEmailAddress());
		Phone phone = new Phone();
		if (StringUtils.isNotBlank(card.getPhoneNumber())) {
			String phoneNumber=StringUtils.remove(card.getPhoneNumber(),HotelConstants.HYPHEN);
			phone.setAreaCode(phoneNumber.substring(0,3));
			phone.setCountryCode(HotelConstants.USA_COUNTRY_CODE_INT);
			phone.setNumber(phoneNumber.substring(3));
		}
		billingContact.setPhone(phone);
		return phone;
	}
	
	public static void setPaymentExpiration(FulFillmentPaymentCard card, Payment payment) {
		if(Objects.nonNull(card.getExpirationDate())) {
			ZoneId defaultZoneId = ZoneId.systemDefault();
			Instant instant = card.getExpirationDate().toInstant();
			LocalDate localDate = instant.atZone(defaultZoneId).toLocalDate();
			payment.setExpirationMonth(String.valueOf(localDate.getMonth().getValue()));
			payment.setExpirationYear(String.valueOf(localDate.getYear())); 
		}
	}
}

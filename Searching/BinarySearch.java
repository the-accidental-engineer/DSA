package Searching;

public class BinarySearch {
    public static double findSquareRoot(int n, double precision) {
        if (n < 0) {
            throw new IllegalArgumentException("Square root is not defined for negative numbers");
        }

        if(n<=1){
            return n;
        }

        double high = n, low = 0, mid =0;

        while((high-low) > precision){
            mid = (high+low)/2;
            if (mid*mid == n){
                return mid;
            } else if(mid*mid < n){
                low = mid;
            } else {
                high = mid;
            }
        }
        return mid;

    }

    public static void main(String[] args) {
        int number = 9; // Input number
        double precision = 0.000000000000000000000000000001; // Desired precision for the square root

        double squareRoot = findSquareRoot(number, precision);
        System.out.println("The square root of " + number + " is approximately: " + squareRoot);
    } 
}


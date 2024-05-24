/* 
Given an integer array nums, move all 0's to the end of it while maintaining the relative order of the non-zero elements.

Note that you must do this in-place without making a copy of the array.

 

Example 1:

Input: nums = [0,1,0,3,12]
Output: [1,3,12,0,0]
Example 2:

Input: nums = [0]
Output: [0]
 

Constraints:

1 <= nums.length <= 104
-231 <= nums[i] <= 231 - 1
 

Follow up: Could you minimize the total number of operations done?
*/

public class Move_Zeroes {

    public static void main(String[] args) {
        int input[] = {1, 8, 7, 56, 90};
        System.out.println("Result :" + Move_Zeroes.secondLargest(input, input.length));

    }

static void moveZeroes(int[] nums) {
        int size = nums.length;
        int k=0;
        int [] tempArr = new int [size];
        for(int i =0; i<size; i++){
            if(nums[i]!=0){
                tempArr[k]=nums[i];
                k++;
            }
        }
        System.arraycopy(tempArr, 0, nums, 0, tempArr.length);
}

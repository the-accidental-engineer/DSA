/*
Given a binary array nums, return the maximum number of consecutive 1's in the array.

 

Example 1:

Input: nums = [1,1,0,1,1,1]
Output: 3
Explanation: The first two digits or the last three digits are consecutive 1s. The maximum number of consecutive 1s is 3.
Example 2:

Input: nums = [1,0,1,1,0,1]
Output: 2
 

Constraints:

1 <= nums.length <= 105
nums[i] is either 0 or 1.

*/

public class MaxConsecutiveOne {

    public static void main(String[] args) {
        Solution solution = new Solution();
        int input[] = { 1, 1, 1, 1, 0, 1, 1, 1 };
        int result = solution.findMaxConsecutiveOnes(input);
        System.out.println("Result :" + result);

    }

}

class Solution {
    public int findMaxConsecutiveOnes(int[] nums) {
        int currCount = 0, maxCount = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == 1) {
                currCount++;
            } else {
                maxCount = Math.max(maxCount, currCount);
                currCount = 0;
            }
        }
        maxCount = Math.max(maxCount, currCount);
        return maxCount;
    }
}

/*
Given a non-empty array of integers nums, every element appears twice except for one. Find that single one.

You must implement a solution with a linear runtime complexity and use only constant extra space.

 

Example 1:

Input: nums = [2,2,1]
Output: 1
Example 2:

Input: nums = [4,1,2,1,2]
Output: 4
Example 3:

Input: nums = [1]
Output: 1
 

Constraints:

1 <= nums.length <= 3 * 104
-3 * 104 <= nums[i] <= 3 * 104

Each element in the array appears twice except for one element which appears only once.
*/

/*  Brute force solution
public int singleNumber(int[] nums) {
        int size = nums.length;
        if(size<2) return nums[0];
        Arrays.sort(nums);
        for(int i=0; i<size-1; i+=2){
            if(nums[i]!=nums[i+1]){
                return nums[i];
            }
        }
        return nums[size-1];
    }
*/

/* Better solution with O(n) time and space complexity(linear)
public int singleNumber(int[] nums) {
        HashMap<Integer, Integer> frequencyMap = new HashMap<>();
        for(int num : nums){
            frequencyMap.put(num, frequencyMap.getOrDefault(num,0)+1);
        }
        for(Map.Entry<Integer,Integer> entry : frequencyMap.entrySet()){
            if(entry.getValue() == 1){
                return entry.getKey();
            }
        }
        return -1;
    }
*/

// Optimal Solution using XOR 
/* 
XOR Operation Basics
The XOR (exclusive OR) operation is a bitwise operator that works on binary digits (bits). Here are the key properties of XOR that are useful for this problem:

XOR of a number with itself is 0:

a ^ a = 0
Example: 5 ^ 5 = 0
XOR of a number with 0 is the number itself:

a ^ 0 = a
Example: 5 ^ 0 = 5
XOR is commutative and associative:

This means the order in which you apply XOR operations does not matter.
a ^ b ^ a = b ^ (a ^ a) = b ^ 0 = b

*/

class Solution {
    public int singleNumber(int[] nums) {
        int result=0;
        for(int num : nums){
            result ^= num;
        }
        return result;
    }
}

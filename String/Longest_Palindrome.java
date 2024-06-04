/*

Given a string s which consists of lowercase or uppercase letters, return the length of the longest 
palindrome
 that can be built with those letters.

Letters are case sensitive, for example, "Aa" is not considered a palindrome.

 

Example 1:

Input: s = "abccccdd"
Output: 7
Explanation: One longest palindrome that can be built is "dccaccd", whose length is 7.
Example 2:

Input: s = "a"
Output: 1
Explanation: The longest palindrome that can be built is "a", whose length is 1.
 

Constraints:

1 <= s.length <= 2000
s consists of lowercase and/or uppercase English letters only.


*/

class Solution {
    public int longestPalindrome(String s) {
        int[] charCounts = new int[128];
        
        for (char c : s.toCharArray()) {
            charCounts[c]++;
        }
        
        int length = 0;
        boolean oddFound = false;
        
        // Iterate over the frequency array
        for (int count : charCounts) {
            if (count % 2 == 0) {
                length += count;
            } else {
                length += count - 1;
                oddFound = true;
            }
        }
        
        // If there was any odd count, we can place one odd character in the center
        if (oddFound) {
            length += 1;
        }
        
        return length;
    }
}

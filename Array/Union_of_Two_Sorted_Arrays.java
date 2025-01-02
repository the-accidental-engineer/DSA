/*
Given two sorted arrays of size n and m respectively, find their union. The Union of two arrays can be defined as the common and distinct elements in the two arrays. Return the elements in sorted order.




Example 1:

Input: 
n = 5, arr1[] = {1, 2, 3, 4, 5}  
m = 5, arr2 [] = {1, 2, 3, 6, 7}
Output: 
1 2 3 4 5 6 7
Explanation: 
Distinct elements including both the arrays are: 1 2 3 4 5 6 7.
Example 2:

Input: 
n = 5, arr1[] = {2, 2, 3, 4, 5}  
m = 5, arr2[] = {1, 1, 2, 3, 4}
Output: 
1 2 3 4 5
Explanation: 
Distinct elements including both the arrays are: 1 2 3 4 5.
Example 3:

Input:
n = 5, arr1[] = {1, 1, 1, 1, 1}
m = 5, arr2[] = {2, 2, 2, 2, 2}
Output: 
1 2
Explanation: 
Distinct elements including both the arrays are: 1 2.
Your Task: 
You do not need to read input or print anything. Complete the function findUnion() that takes two arrays arr1[], arr2[], and their size n and m as input parameters and returns a list containing the union of the two arrays.

Expected Time Complexity: O(n+m).
Expected Auxiliary Space: O(n+m).

Constraints:
1 <= n, m <= 105
-109 <= arr1[i], arr2[i] <= 109
*/



/* Brute Force Solution - It will give LTE 
class Solution
{
    //Function to return a list containing the union of the two arrays.
    public static ArrayList<Integer> findUnion(int arr1[], int arr2[], int n, int m)
    {
        ArrayList<Integer> union = new ArrayList<Integer>();
        for(int i : arr1){
            if(!union.contains(i)){
                union.add(i);
            }
        }
        for(int i : arr2){
            if(!union.contains(i)){
                union.add(i);
            }
        }
         union.sort(Comparator.naturalOrder());
         return union;
    }
}
*/







/* Better Solution using Hashmap 
 class Solution
{
    //Function to return a list containing the union of the two arrays.
    public static ArrayList<Integer> findUnion(int arr1[], int arr2[], int n, int m)
    {
            HashMap<Integer, Boolean> map = new HashMap<>();
            for(int num : arr1){
                map.put(num,true);
            }
            for(int num : arr2){
                map.put(num,true);
            }
            
            ArrayList<Integer> union = new ArrayList<>(map.keySet());
            union.sort(Integer::compareTo);
            return union;
            
    }
}
*/

/*           Optimal Solution using Two Pointer Technique        */

/*

Steps:
1. Initialize Two Pointers: Start pointers at the beginning of both arrays.
2. Traverse Both Arrays: Compare elements at the pointers and add the smaller element to the result.
3. Handle Duplicates: Skip duplicate elements.
4. Add Remaining Elements: After one array is exhausted, add the remaining elements of the other array to the result.


*/


class Solution
{
    //Function to return a list containing the union of the two arrays.
    public static ArrayList<Integer> findUnion(int arr1[], int arr2[], int n, int m)
    {
            ArrayList<Integer> unionList = new ArrayList<Integer>();
            int i=0, j=0;
            
            while(i<n && j<m){
                if(arr1[i]<arr2[j]){
                    if(unionList.isEmpty() || unionList.get(unionList.size()-1) !=arr1[i]){
                        unionList.add(arr1[i]);
                    }
                    i++;
                } else if (arr1[i]>arr2[j]){
                    if(unionList.isEmpty() || unionList.get(unionList.size()-1) !=arr2[j]){
                        unionList.add(arr2[j]);
                    }
                    j++;
                } else {
                    if(unionList.isEmpty() || unionList.get(unionList.size()-1) !=arr1[i]){
                        unionList.add(arr1[i]);
                    }
                    i++;
                    j++;
                }
            }
            
            while (i<n){
                if(unionList.isEmpty() || unionList.get(unionList.size()-1) !=arr1[i]){
                        unionList.add(arr1[i]);
                    }
                    i++;
            }
            
            while (j<m) {
                    if(unionList.isEmpty() || unionList.get(unionList.size()-1) !=arr2[j]){
                        unionList.add(arr2[j]);
                    }
                    j++;
            }
            return unionList;
    }
}


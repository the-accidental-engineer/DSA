/* 



Problem - Largest Element in an Array
//
Given an array A[] of size n. The task is to find the largest element in it.
 //


Example 1:

Input:
n = 5
A[] = {1, 8, 7, 56, 90}
Output:
90
Explanation:
The largest element of given array is 90.
 

Example 2:

Input:
n = 7
A[] = {1, 2, 0, 3, 2, 4, 5}
Output:
5
Explanation:
The largest element of given array is 5.
 

Your Task:  
You don't need to read input or print anything. Your task is to complete the function largest() which takes the array A[] and its size n as inputs and returns the maximum element in the array.

 

Expected Time Complexity: O(N)
Expected Auxiliary Space: O(1)

 

Constraints:
1 <= n<= 103
0 <= A[i] <= 103
Array may contain duplicate elements.  


*/


public class Largest_Num_Array {

    public static void main(String[] args) {
        int input[] = {1, 8, 7, 56, 90};
        System.out.println("Result :" + Largest_Num_Array.largest(input, input.length));

    }

    static int largest(int arr[], int n) {
        int largest = arr[0];
        for (int i = 1; i < n; i++) {
            if (largest < arr[i]) {
                largest = arr[i];
            }
        }
        return largest;
    }

}

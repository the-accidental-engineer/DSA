package Basics;

import java.util.List;

public class ArrayUtil {

    public void ArrayDemo() {
        int[] arr = new int[] { -1, 2, 3, 4, 5, 6, 7, 8, 9, -10 }; // Declare and Intialization
        System.out.println((findMin(arr)));
    }

    public void printArray(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
    }

    public void printEvenUsingList() {
        List.of(12, 34, 67, 19, 32, 4)
                .stream()
                .filter(element -> element % 2 == 0)
                .forEach(
                        element -> System.out.println(element));
    }

    public void printEven(int[] arr) {
        for (int num : arr) {
            if (num % 2 == 0) {
                System.out.print(num + " ");
            }
        }
    }

    public int[] removeEven(int[] arr) {
        int oddCount = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] % 2 != 0) {
                oddCount++;
            }
        }
        int[] result = new int[oddCount];
        int j = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] % 2 != 0) {
                result[j] = arr[i];
                j++;
            }

        }
        return result;

    }

    public int[] reverseArray(int[] arr) {
        int start = 0;
        int end = arr.length - 1;
        while (start < end) {
            int temp = arr[start];
            arr[start] = arr[end];
            arr[end] = temp;
            start++;
            end--;
        }
        return arr;
    }

    public int findMin(int[] arr) {
        int min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }

    public static void main(String[] args) {
        ArrayUtil arrUtil = new ArrayUtil();
        arrUtil.ArrayDemo();
    }
}
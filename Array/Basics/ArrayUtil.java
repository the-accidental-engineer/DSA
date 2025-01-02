package Basics;

import java.util.List;

public class ArrayUtil {

    public void printArray(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
    }

    public void ArrayDemo() {
        int[] arr = new int[] { 1, 2, 3, 4 }; // Declare and Intialization
        printEven(arr);
    }

    // public void printEven (){
    // List.of(12, 34, 67, 19, 32, 4)
    // .stream()
    // .filter(element -> element % 2 == 0)
    // .forEach(
    // element -> System.out.println(element));
    // }

    public void printEven(int[] arr) {
        for (int num : arr) {
            if (num % 2 == 0) {
                System.out.print(num + " ");
            }
        }
    }

    public static void main(String[] args) {
        ArrayUtil arrUtil = new ArrayUtil();
        arrUtil.ArrayDemo();
    }
}
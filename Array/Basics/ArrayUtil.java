package Basics;

import java.util.List;

public class ArrayUtil {

    public void printArray (int[] arr){
        for (int i =0; i<arr.length; i++){
            System.out.print(arr[i] + " ");
        }  
    }

    public void dummyArray (){
        int [] arr = new int [] {1,2,3,4}; // Declare and Intialization
        printArray(arr);
    }

    public void printEven (){
        List.of(12, 34, 67, 19, 32, 4)
        .stream()
        .filter(element -> element % 2 == 0)
        .forEach(
            element -> System.out.println(element));
    }

    public static void main (String[] args){
        ArrayUtil arrUtil = new ArrayUtil ();
        arrUtil.printEven(); 
    }
}
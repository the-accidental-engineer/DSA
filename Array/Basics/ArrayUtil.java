package Basics;

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

    public static void main (String[] args){
        ArrayUtil arrUtil = new ArrayUtil ();
        arrUtil.dummyArray(); 
    }
}
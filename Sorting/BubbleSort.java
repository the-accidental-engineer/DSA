package Sorting;

import java.util.Arrays;

public class BubbleSort {
    public static int [] BubbleSortMethod(int [] arr ){
        for(int i = arr.length-1; i>0; i--){
            for(int j = 0; j<i; j++){
                if(arr[j]>arr[j+1]){
                    int temp = arr[j];
                    arr[j] = arr[j+1];
                    arr[j+1] = temp;
                }
            }
        }
        return arr;
    }

    public static void main(String [] args){
        int [] input = {5,6,7,1,3,2,4,8,9,10};
        System.out.println(Arrays.toString(BubbleSortMethod(input)));

    }
}

package Searching;

public class LinearSearch {
    public static boolean isExist(int[] input,int target){
        for( int num : input){
            if(num == target){
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int [] input = {1,2,3,4,5,6,7,8,9,0,};
        System.out.println(isExist(input, 1));
    }
}

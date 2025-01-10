import java.util.Arrays;

public class ResizeArray {
    public static void main(String[] args) {
        ResizeArray resizeArray = new ResizeArray();
        int [] input = new int[]{1,2,3,4,5,6,7,8,9,10};
        int [] result = resizeArray.ResizeArrayFunction(input,5);
        System.out.println("Input :" + Arrays.toString(input) + " Size : " + input.length);
        System.out.println("Input :" + Arrays.toString(result) + " Size : " + result.length);
    }

    public int [] ResizeArrayFunction(int[] arr, int newCapacity){
        //return Arrays.copyOfRange(arr, 0, newCapacity);
        int [] temp = new int[newCapacity];
        for(int i=0; i<newCapacity; i++){
            temp[i] = arr[i];
        }
        return temp;
    }
}

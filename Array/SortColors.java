import java.util.Arrays;

public class SortColors {
    public static int[] merge(int[] arr1, int[] arr2) {
        int[] combinedArray = new int[arr1.length + arr2.length];
        int i = 0, j = 0, combinedIndex = 0;
        while (i < arr1.length && j < arr2.length) {
            if (arr1[i] < arr2[j]) {
                combinedArray[combinedIndex] = arr1[i];
                i++;
                combinedIndex++;
            } else {
                combinedArray[combinedIndex] = arr2[j];
                j++;
                combinedIndex++;
            }
        }
        while (i < arr1.length) {
            combinedArray[combinedIndex] = arr1[i];
            i++;
            combinedIndex++;
        }
        while (j < arr2.length) {
            combinedArray[combinedIndex] = arr2[j];
            j++;
            combinedIndex++;
        }
        return combinedArray;
    }

    public static int [] MergeSort(int [] input){
        if(input.length==1){return input;}
        int mid = input.length/2;
        int [] left = MergeSort(Arrays.copyOfRange(input,0, mid));
        int [] right = MergeSort(Arrays.copyOfRange(input,mid, input.length));
        return merge(left, right);
    }

    public static void main(String [] args){
        int [] unSortedArray = {2,0,2,1,1,0};
        System.out.println(Arrays.toString(MergeSort(unSortedArray)));
    }
}

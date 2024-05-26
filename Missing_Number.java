class Missing_Number {
    public int missingNumber(int[] nums) {
        int size = nums.length;
        int sum = (size * (size + 1)) / 2;
        for (int val : nums) {
            sum -= val;
        }
        return sum;

        
    }
}

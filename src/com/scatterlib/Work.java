package com.scatterlib;

public class Work {

    public static int work(Integer[] numbers) {
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return sum * 1;
    }

}

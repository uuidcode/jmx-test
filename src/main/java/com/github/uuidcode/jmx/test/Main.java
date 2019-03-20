package com.github.uuidcode.jmx.test;

public class Main {
    public static void main(String[] args){
        while (true) {
            try {
                Thread.sleep(10 * 1000);
            } catch (Throwable t) {
            }

            System.out.println(System.currentTimeMillis());
        }
    }
}

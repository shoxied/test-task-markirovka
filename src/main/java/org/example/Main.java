package org.example;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5, "");
        String res = crptApi.createDocument("", "");
        System.out.println(res);
    }
}
package com.aishoppingguide.allinone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aishoppingguide")
public class AllInOneApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllInOneApplication.class, args);
    }
}

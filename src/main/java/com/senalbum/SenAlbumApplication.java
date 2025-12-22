package com.senalbum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SenAlbumApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenAlbumApplication.class, args);
    }
}

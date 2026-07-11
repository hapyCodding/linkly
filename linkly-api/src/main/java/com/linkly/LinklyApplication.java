package com.linkly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LinklyApplication {
    public static void main(String[] args) {
        SpringApplication.run(LinklyApplication.class, args);
    }
}

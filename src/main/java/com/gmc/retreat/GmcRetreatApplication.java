package com.gmc.retreat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GmcRetreatApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmcRetreatApplication.class, args);
    }
}

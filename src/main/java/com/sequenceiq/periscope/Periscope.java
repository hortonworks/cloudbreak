package com.sequenceiq.periscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.periscope")
public class Periscope {

    public static void main(String[] args) {
        if (args.length == 0) {
            SpringApplication.run(Periscope.class);
        } else {
            SpringApplication.run(Periscope.class, args);
        }
    }

}

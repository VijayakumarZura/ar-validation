package com.zura.ar_rules.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @Value("${app.timezone}")
    private String timeZone;

    @PostConstruct
    public void init() {
        // Set the default time zone
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
        System.out.println("Spring Boot application running in time zone: " + timeZone);
    }

    @Bean
    public TimeZone timeZone() {
        return TimeZone.getTimeZone(timeZone);
    }
}

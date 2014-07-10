package com.sequenceiq.periscope.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableScheduling
public class AppConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private List<Converter> converters;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        for (Converter converter : converters) {
            registry.addConverter(converter);
        }
    }

    @Bean
    ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
        return new ThreadPoolExecutorFactoryBean();
    }
}

package com.sequenceiq.cloudbreak.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.controller.mapper.TypeAwareExceptionMapper;

@Configuration
public class ExceptionMapperConfig {

    @Inject
    private List<TypeAwareExceptionMapper<? extends Throwable>> exceptionMappers;

    @Bean
    public Map<Class<? extends Throwable>, TypeAwareExceptionMapper<Throwable>> getExceptionMappersByClass() {
        Map<Class<? extends Throwable>, TypeAwareExceptionMapper<Throwable>> mappersByClass = new HashMap<>();
        for (TypeAwareExceptionMapper<? extends Throwable> mapper : exceptionMappers) {
            mappersByClass.put(mapper.supportedType(), (TypeAwareExceptionMapper<Throwable>) mapper);
        }
        return mappersByClass;
    }
}

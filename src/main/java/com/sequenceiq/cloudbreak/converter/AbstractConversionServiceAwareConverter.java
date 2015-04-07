package com.sequenceiq.cloudbreak.converter;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

public abstract class AbstractConversionServiceAwareConverter<S, T> implements Converter<S, T> {
    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public ConversionService getConversionService() {
        return conversionService;
    }

    @PostConstruct
    private void register() {
        if (conversionService instanceof ConverterRegistry) {
            ((ConverterRegistry) conversionService).addConverter(this);
        } else {
            throw new IllegalStateException("Can't register Converter to ConverterRegistry");
        }
    }
}

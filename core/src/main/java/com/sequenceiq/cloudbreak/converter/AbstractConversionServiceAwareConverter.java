package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

public abstract class AbstractConversionServiceAwareConverter<S, T> implements Converter<S, T> {
    @Inject
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

    public List<T> convert(Collection<S> sources) {
        List<T> targets = new ArrayList<>();
        if (sources != null) {
            for (S source : sources) {
                targets.add(convert(source));
            }
        }
        return targets;
    }
}

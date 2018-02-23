package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.google.common.base.Strings;

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

    public List<T> convert(Iterable<S> sources) {
        List<T> targets = new ArrayList<>();
        if (sources != null) {
            for (S source : sources) {
                targets.add(convert(source));
            }
        }
        return targets;
    }

    protected Map<String, Object> cleanMap(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        for (Entry<String, Object> entry : input.entrySet()) {
            if (!Objects.isNull(input.get(entry.getKey()))
                    && !"null".equals(input.get(entry.getKey()))
                    && !Strings.isNullOrEmpty(input.get(entry.getKey()).toString())) {
                result.put(entry.getKey(), input.get(entry.getKey()));
            }
        }
        return result;
    }
}

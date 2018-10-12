package com.sequenceiq.cloudbreak.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ConverterUtil {

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public <T> List<T> convertAll(Iterable<? extends Object> list, Class<T> clss) {
        return Stream.of(list)
                .map(event -> conversionService.convert(event, clss))
                .collect(Collectors.toList());
    }

    public <T> List<T> convertAll(Collection<? extends Object> list, Class<T> clss) {
        return list.stream()
                .map(event -> conversionService.convert(event, clss))
                .collect(Collectors.toList());
    }

    public <T> T convert(Object object, Class<T> clss) {
        return conversionService.convert(object, clss);
    }
}

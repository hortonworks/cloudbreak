package com.sequenceiq.cloudbreak.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ConverterUtil {

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public <T> List<T> convertAll(Iterable<?> list, Class<T> clss) {
        return StreamSupport.stream(list.spliterator(), false)
                .map(event -> conversionService.convert(event, clss))
                .collect(Collectors.toList());
    }

    public <T> List<T> convertAll(Collection<?> list, Class<T> clss) {
        return list.stream()
                .map(event -> conversionService.convert(event, clss))
                .collect(Collectors.toList());
    }

    public <T> Set<T> convertAllAsSet(Collection<?> list, Class<T> clss) {
        return list.stream()
                .map(event -> conversionService.convert(event, clss))
                .collect(Collectors.toSet());
    }

    public <T> T convert(Object object, Class<T> clss) {
        return conversionService.convert(object, clss);
    }
}

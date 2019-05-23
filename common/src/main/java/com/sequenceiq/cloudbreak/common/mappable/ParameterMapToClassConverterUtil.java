package com.sequenceiq.cloudbreak.common.mappable;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class ParameterMapToClassConverterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterMapToClassConverterUtil.class);

    private static final String CONVERT_EXCEPTION_MESSAGE_FORMAT = "Unable to deserialize %s from parameters";

    @Inject
    private ConversionService conversionService;

    public static <R> R exec(Callable<R> method, Class<R> clazz) {
        try {
            return method.call();
        } catch (Exception e) {
            String message = String.format(CONVERT_EXCEPTION_MESSAGE_FORMAT, clazz.getSimpleName());
            LOGGER.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

    public <R> R exec(Object object, Class<R> clazz) {
        try {
            return conversionService.convert(object, clazz);
        } catch (Exception e) {
            String message = String.format(CONVERT_EXCEPTION_MESSAGE_FORMAT, clazz.getSimpleName());
            LOGGER.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

}

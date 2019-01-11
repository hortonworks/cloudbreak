package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterMapToClassConverterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterMapToClassConverterUtil.class);

    private static final String CONVERT_EXCEPTION_MESSAGE_FORMAT = "Unable to deserialize %s from parameters";

    private ParameterMapToClassConverterUtil() {
    }

    public static <R> R exec(Callable<R> method, Class<R> clazz) {
        try {
            return method.call();
        } catch (Exception e) {
            String message = String.format(CONVERT_EXCEPTION_MESSAGE_FORMAT, clazz.getSimpleName());
            LOGGER.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

}

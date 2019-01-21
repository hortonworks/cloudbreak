package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;

@Component
public class ParameterMapToClassConverterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterMapToClassConverterUtil.class);

    private static final String CONVERT_EXCEPTION_MESSAGE_FORMAT = "Unable to deserialize %s from parameters";

    @Inject
    private ConverterUtil converterUtil;

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
            return converterUtil.convert(object, clazz);
        } catch (Exception e) {
            String message = String.format(CONVERT_EXCEPTION_MESSAGE_FORMAT, clazz.getSimpleName());
            LOGGER.error(message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

}

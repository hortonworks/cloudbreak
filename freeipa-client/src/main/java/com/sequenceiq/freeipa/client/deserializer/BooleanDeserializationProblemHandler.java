package com.sequenceiq.freeipa.client.deserializer;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;

public class BooleanDeserializationProblemHandler extends DeserializationProblemHandler {

    @Override
    public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) throws IOException {
        if (targetType == Boolean.class) {
            return Boolean.valueOf(StringUtils.lowerCase(valueToConvert));
        }
        return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg);
    }
}

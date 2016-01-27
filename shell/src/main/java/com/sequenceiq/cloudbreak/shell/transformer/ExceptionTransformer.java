package com.sequenceiq.cloudbreak.shell.transformer;

import org.springframework.stereotype.Component;

@Component
public class ExceptionTransformer {

    public RuntimeException transformToRuntimeException(Exception e) {
        return new RuntimeException(e.getMessage());
    }

    public RuntimeException transformToRuntimeException(String errorMessage) {
        return new RuntimeException(errorMessage);
    }
}

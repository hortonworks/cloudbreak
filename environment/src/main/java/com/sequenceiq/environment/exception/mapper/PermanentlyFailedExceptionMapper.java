package com.sequenceiq.environment.exception.mapper;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.PermanentlyFailedException;

@Component
    public class PermanentlyFailedExceptionMapper extends SearchCauseExceptionMapper<PermanentlyFailedException> {

    @Override
    public Class<PermanentlyFailedException> getExceptionType() {
        return PermanentlyFailedException.class;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}

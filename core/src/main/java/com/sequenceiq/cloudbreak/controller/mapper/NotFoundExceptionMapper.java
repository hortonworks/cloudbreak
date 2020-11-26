package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.NotFoundException;

import ch.qos.logback.classic.Level;

@Component
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    Status getResponseStatus(NotFoundException exception) {
        return Status.NOT_FOUND;
    }

    @Override
    Class<NotFoundException> getExceptionType() {
        return NotFoundException.class;
    }

    @Override
    protected String getErrorMessage(NotFoundException exception) {
        return "Resource not found: " + super.getErrorMessage(exception);
    }

    @Override
    protected Level getLogLevel() {
        return Level.INFO;
    }
}

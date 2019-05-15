package com.sequenceiq.datalake.controller.mapper;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.DEBUG_INT;
import static ch.qos.logback.classic.Level.ERROR_INT;
import static ch.qos.logback.classic.Level.INFO_INT;
import static ch.qos.logback.classic.Level.WARN_INT;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.datalake.api.model.ExceptionResponse;

import ch.qos.logback.classic.Level;

abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionMapper.class);

    @Override
    public Response toResponse(E exception) {
        if (logException()) {
            String errorMessage = getErrorMessage(exception);
            switch (getLogLevel().levelInt) {
                case ERROR_INT:
                    LOGGER.error(errorMessage, exception);
                    break;
                case WARN_INT:
                    LOGGER.warn(errorMessage, exception);
                    break;
                case INFO_INT:
                    LOGGER.info(errorMessage, exception);
                    break;
                case DEBUG_INT:
                    LOGGER.debug(errorMessage, exception);
                    break;
                default:
                    LOGGER.info(errorMessage, exception);
                    break;
            }
        }
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }

    protected String getErrorMessage(E exception) {
        String message = exception.getMessage();
        LOGGER.debug("Exception text has been mapped: {}", message);
        return message;
    }

    protected String getErrorMessageFromThrowable(Throwable e) {
        if (getExceptionType().equals(e.getClass())) {
            return getErrorMessage((E) e);
        }
        LOGGER.error("Invalid exception type was used, {} != {}", e.getClass(), getExceptionType());
        return e.getMessage();
    }

    protected Object getEntity(E exception) {
        return new ExceptionResponse(getErrorMessage(exception));
    }

    protected boolean logException() {
        return true;
    }

    protected Level getLogLevel() {
        return DEBUG;
    }

    abstract Status getResponseStatus();

    abstract Class<E> getExceptionType();
}

package com.sequenceiq.environment.exception.mapper;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.DEBUG_INT;
import static ch.qos.logback.classic.Level.ERROR_INT;
import static ch.qos.logback.classic.Level.INFO_INT;
import static ch.qos.logback.classic.Level.WARN_INT;
import static com.sequenceiq.environment.util.Validation.notNull;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.env.api.model.ExceptionResponse;

import ch.qos.logback.classic.Level;

abstract class BaseExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionMapper.class);

    @Override
    public Response toResponse(T exception) {
        if (logException()) {
            String errorMessage = getErrorMessage(exception);
            switch (getLogLevel().levelInt) {
                case ERROR_INT:
                    LOGGER.error(errorMessage, exception);
                    break;
                case WARN_INT:
                    LOGGER.warn(errorMessage, exception);
                    break;
                case DEBUG_INT:
                    LOGGER.debug(errorMessage, exception);
                    break;
                case INFO_INT:
                default:
                    LOGGER.info(errorMessage, exception);
                    break;
            }
        }
        return Response.status(getResponseStatus()).entity(getEntity(exception)).build();
    }

    @SuppressWarnings("unchecked")
    protected String getErrorMessageFromThrowable(Throwable t) {
        notNull(t, "throwable");
        if (t.getClass().equals(getExceptionType())) {
            return getErrorMessage((T) t);
        }
        LOGGER.error("Invalid exception type was used, {} != {}", t.getClass(), getExceptionType());
        return t.getMessage();
    }

    protected Object getEntity(T exception) {
        return new ExceptionResponse(getErrorMessage(exception));
    }

    abstract Status getResponseStatus();

    abstract Class<T> getExceptionType();

    private String getErrorMessage(T throwable) {
        notNull(throwable, "throwable");
        String message = throwable.getMessage();
        LOGGER.debug("Exception text has been mapped: {}", message);
        return message;
    }

    private boolean logException() {
        return true;
    }

    private Level getLogLevel() {
        return DEBUG;
    }

}

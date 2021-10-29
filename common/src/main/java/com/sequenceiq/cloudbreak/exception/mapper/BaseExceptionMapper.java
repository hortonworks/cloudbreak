package com.sequenceiq.cloudbreak.exception.mapper;

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

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;

import ch.qos.logback.classic.Level;

public abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

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
        return Response.status(getResponseStatus(exception)).entity(getEntity(exception)).build();
    }

    protected String getErrorMessage(E exception) {
        String message = exception.getMessage();
        LOGGER.debug("Exception text has been mapped: {}", message);
        return message;
    }

    public String getErrorMessageFromThrowable(Throwable e) {
        if (getExceptionType().equals(e.getClass())) {
            return getErrorMessage((E) e);
        }
        LOGGER.error("Invalid exception type was used, {} != {}", e.getClass(), getExceptionType());
        return e.getMessage();
    }

    /**
     * The error message should be consistent. The response generates the message json from the entity and If we use different java class then the result will
     * different. For example:
     * Entity class: ExceptionResponse -> {"message":"error message"}
     * Entity class: ValidationResult -> {"validationErrors":{"getByCrn.arg0":"Invalid request object"}}
     * It is very hard to process on the client side. So we should respond with the ExceptionResponse and add the payload if it is available.
     */
    protected final ExceptionResponse getEntity(E exception) {
        return new ExceptionResponse(getErrorMessage(exception), getPayload(exception));
    }

    protected Object getPayload(E exception) {
        return null;
    }

    protected boolean logException() {
        return true;
    }

    protected Level getLogLevel() {
        return DEBUG;
    }

    public abstract Status getResponseStatus(E exception);

    public abstract Class<E> getExceptionType();
}

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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;

import ch.qos.logback.classic.Level;

/**
 * We can add the superclass as a {@code T}, the @{code {@link org.glassfish.jersey.spi.ExceptionMappers}} will find the closest type to the mapped exception.
 */
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
                    LOGGER.info(errorMessage);
                    break;
                default:
                    LOGGER.info(errorMessage, exception);
                    break;
            }
        }
        return Response.status(getResponseStatus(exception)).entity(getEntity(exception)).build();
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

    abstract Status getResponseStatus(T exception);

    abstract Class<T> getExceptionType();

    protected String getErrorMessage(T throwable) {
        notNull(throwable, "throwable");
        return ExceptionUtils.getRootCause(throwable).getMessage();
    }

    protected boolean logException() {
        return true;
    }

    protected Level getLogLevel() {
        return DEBUG;
    }

}

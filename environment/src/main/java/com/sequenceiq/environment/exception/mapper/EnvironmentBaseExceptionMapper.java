package com.sequenceiq.environment.exception.mapper;

import static com.sequenceiq.environment.util.Validation.notNull;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

/**
 * We can add the superclass as a {@code T}, the @{code {@link org.glassfish.jersey.spi.ExceptionMappers}} will find the closest type to the mapped exception.
 */
abstract class EnvironmentBaseExceptionMapper<T extends Throwable> extends BaseExceptionMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseExceptionMapper.class);

    @SuppressWarnings("unchecked")
    public String getErrorMessageFromThrowable(Throwable t) {
        notNull(t, "throwable");
        if (t.getClass().equals(getExceptionType())) {
            return getErrorMessage((T) t);
        }
        LOGGER.error("Invalid exception type was used, {} != {}", t.getClass(), getExceptionType());
        return t.getMessage();
    }

    protected String getErrorMessage(T throwable) {
        notNull(throwable, "throwable");
        return ExceptionUtils.getRootCause(throwable).getMessage();
    }
}

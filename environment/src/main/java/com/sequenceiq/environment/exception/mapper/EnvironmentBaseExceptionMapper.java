package com.sequenceiq.environment.exception.mapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

/**
 * We can add the superclass as a {@code T}, the @{code {@link org.glassfish.jersey.spi.ExceptionMappers}} will find the closest type to the mapped exception.
 */
abstract class EnvironmentBaseExceptionMapper<T extends Throwable> extends BaseExceptionMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBaseExceptionMapper.class);

    protected String getErrorMessage(T throwable) {
        Validate.notNull(throwable, "Throwable should not be null!");
        Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        String message = rootCause.getMessage();
        return StringUtils.isNotEmpty(message) ? message : rootCause.getClass().getSimpleName();
    }
}

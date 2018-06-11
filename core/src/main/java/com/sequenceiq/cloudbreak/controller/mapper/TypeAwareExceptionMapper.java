package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.ext.ExceptionMapper;

public interface TypeAwareExceptionMapper<E extends Throwable> extends ExceptionMapper<E> {
    Class<E> supportedType();
}

package com.sequenceiq.cloudbreak.controller.mapper;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;

@Component
public class TransactionRuntimeExecutionExceptionMapper extends SendNotificationExceptionMapper<TransactionRuntimeExecutionException> {

    private static final ThreadLocal<TransactionRuntimeExecutionException> CURRENT_EXCEPTION = new ThreadLocal<>();

    private final List<BaseExceptionMapper<?>> exceptionMappers;

    public TransactionRuntimeExecutionExceptionMapper(List<BaseExceptionMapper<?>> exceptionMappers) {
        this.exceptionMappers = exceptionMappers;
    }

    @Override
    public Response toResponse(TransactionRuntimeExecutionException exception) {
        try {
            CURRENT_EXCEPTION.set(exception);
            return super.toResponse(exception);
        } finally {
            CURRENT_EXCEPTION.remove();
        }
    }

    @Override
    Status getResponseStatus() {
        return exceptionMappers.stream()
            .filter(m -> m.getExceptionType().equals(CURRENT_EXCEPTION.get().getCause().getCause().getClass()))
            .map(BaseExceptionMapper::getResponseStatus)
            .findFirst().orElse(Status.INTERNAL_SERVER_ERROR);
    }

    @Override
    Class<TransactionRuntimeExecutionException> getExceptionType() {
        return TransactionRuntimeExecutionException.class;
    }

    @Override
    protected String getErrorMessage(TransactionRuntimeExecutionException exception) {
        return exception.getCause().getCause().getMessage();
    }
}
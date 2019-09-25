package com.sequenceiq.cloudbreak.controller.mapper;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;

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
        TransactionRuntimeExecutionException deepest = getDeepestTransactionException(CURRENT_EXCEPTION.get());
        return exceptionMappers.stream()
                .filter(m -> m.getExceptionType().equals(deepest.getOriginalCause().getClass()))
                .map(BaseExceptionMapper::getResponseStatus)
                .findFirst().orElse(Status.INTERNAL_SERVER_ERROR);
    }

    private TransactionRuntimeExecutionException getDeepestTransactionException(TransactionRuntimeExecutionException e) {
        if (!(e.getCause().getCause() instanceof TransactionRuntimeExecutionException)) {
            return e;
        }
        return getDeepestTransactionException((TransactionRuntimeExecutionException) e.getCause().getCause());
    }

    @Override
    Class<TransactionRuntimeExecutionException> getExceptionType() {
        return TransactionRuntimeExecutionException.class;
    }

    @Override
    protected String getErrorMessage(TransactionRuntimeExecutionException exception) {
        TransactionRuntimeExecutionException deepest = getDeepestTransactionException(exception);
        return exceptionMappers.stream()
                .filter(mapper -> mapper.getExceptionType().equals(deepest.getOriginalCause().getClass()))
                .map(mapper -> mapper.getErrorMessageFromThrowable(deepest.getOriginalCause()))
                .findFirst().orElse(deepest.getOriginalCause().getMessage());
    }

    @Override
    protected boolean logException() {
        boolean logException = true;
        TransactionRuntimeExecutionException deepest = getDeepestTransactionException(CURRENT_EXCEPTION.get());
        if (deepest.getOriginalCause().getClass().getSimpleName().contains("NotFound")) {
            logException = false;
        }
        return logException;
    }
}
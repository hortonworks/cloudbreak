package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;

@Provider
public class TransactionRuntimeExecutionExceptionMapper extends BaseExceptionMapper<TransactionRuntimeExecutionException> {

    @Override
    Status getResponseStatus() {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    protected String getErrorMessage(TransactionRuntimeExecutionException exception) {
        return exception.getCause().getCause().getMessage();
    }
}
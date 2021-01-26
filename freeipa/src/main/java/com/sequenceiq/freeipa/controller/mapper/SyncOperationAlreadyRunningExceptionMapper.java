package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.controller.exception.SyncOperationAlreadyRunningException;

@Component
public class SyncOperationAlreadyRunningExceptionMapper extends BaseExceptionMapper<SyncOperationAlreadyRunningException> {

    @Override
    Status getResponseStatus(SyncOperationAlreadyRunningException exception) {
        return Status.CONFLICT;
    }

    @Override
    Class<SyncOperationAlreadyRunningException> getExceptionType() {
        return SyncOperationAlreadyRunningException.class;
    }

}
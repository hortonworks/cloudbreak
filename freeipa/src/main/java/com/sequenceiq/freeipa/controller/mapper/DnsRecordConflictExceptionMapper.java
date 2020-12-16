package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordConflictException;

@Component
public class DnsRecordConflictExceptionMapper extends BaseExceptionMapper<DnsRecordConflictException> {

    @Override
    protected Object getEntity(DnsRecordConflictException exception) {
        return new ExceptionResponse("Error during DNS record operation: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }

    @Override
    Class<DnsRecordConflictException> getExceptionType() {
        return DnsRecordConflictException.class;
    }
}

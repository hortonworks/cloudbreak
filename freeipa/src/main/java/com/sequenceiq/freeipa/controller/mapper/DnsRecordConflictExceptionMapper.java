package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsRecordConflictException;

@Component
public class DnsRecordConflictExceptionMapper extends BaseExceptionMapper<DnsRecordConflictException> {

    @Override
    protected String getErrorMessage(DnsRecordConflictException exception) {
        return "Error during DNS record operation: " + exception.getMessage();
    }

    @Override
    public Status getResponseStatus(DnsRecordConflictException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<DnsRecordConflictException> getExceptionType() {
        return DnsRecordConflictException.class;
    }
}

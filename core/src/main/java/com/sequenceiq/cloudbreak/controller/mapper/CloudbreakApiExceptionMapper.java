package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;

@Component
public class CloudbreakApiExceptionMapper extends SendNotificationExceptionMapper<CloudbreakApiException> {

    @Override
    public Status getResponseStatus(CloudbreakApiException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<CloudbreakApiException> getExceptionType() {
        return CloudbreakApiException.class;
    }
}

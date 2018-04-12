package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class CloudbreakApiExceptionMapper extends SendNotificationExceptionMapper<CloudbreakApiException> {

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }
}

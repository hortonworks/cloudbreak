package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;

@Provider
public class CloudbreakApiExceptionMapper extends SendNotificationExceptionMapper<CloudbreakApiException> {

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }
}

package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.service.subscription.SubscriptionAlreadyExistException;

@Provider
public class SubscriptionAlreadyExistExceptionMapper extends BaseExceptionMapper<SubscriptionAlreadyExistException> {

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }
}

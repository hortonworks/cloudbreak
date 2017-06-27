package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.service.subscription.SubscriptionAlreadyExistException;

@Provider
public class SubscriptionAlreadyExistExceptionMapper extends BaseExceptionMapper<SubscriptionAlreadyExistException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.CONFLICT;
    }
}

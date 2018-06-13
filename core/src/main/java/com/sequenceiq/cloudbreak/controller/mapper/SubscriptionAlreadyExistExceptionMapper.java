package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.subscription.SubscriptionAlreadyExistException;

@Component
public class SubscriptionAlreadyExistExceptionMapper extends BaseExceptionMapper<SubscriptionAlreadyExistException> {

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }

    @Override
    Class<SubscriptionAlreadyExistException> getExceptionType() {
        return SubscriptionAlreadyExistException.class;
    }
}

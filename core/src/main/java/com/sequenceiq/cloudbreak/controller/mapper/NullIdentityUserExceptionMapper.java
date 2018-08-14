package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.user.NullIdentityUserException;

@Component
public class NullIdentityUserExceptionMapper extends BaseExceptionMapper<NullIdentityUserException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }

    @Override
    Class<NullIdentityUserException> getExceptionType() {
        return NullIdentityUserException.class;
    }
}

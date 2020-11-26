package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    Status getResponseStatus(AccessDeniedException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    Class<AccessDeniedException> getExceptionType() {
        return AccessDeniedException.class;
    }
}

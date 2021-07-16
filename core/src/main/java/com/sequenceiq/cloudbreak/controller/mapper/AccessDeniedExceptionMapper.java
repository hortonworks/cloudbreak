package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class AccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    public Status getResponseStatus(AccessDeniedException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<AccessDeniedException> getExceptionType() {
        return AccessDeniedException.class;
    }
}

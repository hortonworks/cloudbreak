package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;

@Component
public class CloudbreakImageCatalogExceptionMapper extends SendNotificationExceptionMapper<CloudbreakImageCatalogException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }

    @Override
    Class<CloudbreakImageCatalogException> getExceptionType() {
        return CloudbreakImageCatalogException.class;
    }
}
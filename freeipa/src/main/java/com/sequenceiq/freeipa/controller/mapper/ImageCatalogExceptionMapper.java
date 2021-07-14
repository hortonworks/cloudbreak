package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.service.image.ImageCatalogException;

@Component
public class ImageCatalogExceptionMapper extends BaseExceptionMapper<ImageCatalogException> {

    @Override
    public Status getResponseStatus(ImageCatalogException exception) {
        return Status.NOT_FOUND;
    }

    @Override
    public Class<ImageCatalogException> getExceptionType() {
        return ImageCatalogException.class;
    }
}
package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.service.image.ImageNotFoundException;

@Component
public class ImageNotFoundExceptionMapper extends BaseExceptionMapper<ImageNotFoundException> {

    @Override
    Status getResponseStatus(ImageNotFoundException exception) {
        return Status.NOT_FOUND;
    }

    @Override
    Class<ImageNotFoundException> getExceptionType() {
        return ImageNotFoundException.class;
    }
}
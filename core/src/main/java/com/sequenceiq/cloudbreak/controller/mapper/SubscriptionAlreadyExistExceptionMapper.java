package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.subscription.SubscriptionAlreadyExistException;

@Provider
public class SubscriptionAlreadyExistExceptionMapper implements ExceptionMapper<SubscriptionAlreadyExistException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionAlreadyExistExceptionMapper.class);

    @Override
    public Response toResponse(SubscriptionAlreadyExistException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.CONFLICT).entity(new ExceptionResult(exception.getMessage()))
                .build();
    }
}

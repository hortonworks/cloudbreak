package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Provider
public class HttpRequestMethodNotSupportedExceptionMapper implements ExceptionMapper<HttpRequestMethodNotSupportedException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestMethodNotSupportedExceptionMapper.class);

    @Override
    public Response toResponse(HttpRequestMethodNotSupportedException exception) {
        MDCBuilder.buildMdcContext();
        LOGGER.error(exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ExceptionResult("The requested http method is not supported on the resource.")).build();
    }
}

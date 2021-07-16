package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

import io.grpc.StatusRuntimeException;

@Component
public class StatusRuntimeExceptionMapper extends BaseExceptionMapper<StatusRuntimeException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusRuntimeExceptionMapper.class);

    @Override
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public Response.Status getResponseStatus(StatusRuntimeException exception) {
        switch (exception.getStatus().getCode()) {
            case OK:
                return Response.Status.OK;
            case NOT_FOUND:
                return Response.Status.NOT_FOUND;
            case PERMISSION_DENIED:
                return Response.Status.FORBIDDEN;
            case UNIMPLEMENTED:
                return Response.Status.NOT_IMPLEMENTED;
            case UNAVAILABLE:
                return Response.Status.SERVICE_UNAVAILABLE;
            case UNAUTHENTICATED:
                return Response.Status.UNAUTHORIZED;
            default:
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }

    @Override
    protected String getErrorMessage(StatusRuntimeException exception) {
        LOGGER.info("Error occurred in gRPC call: {}, original message: {}", exception.getStatus(), exception.getMessage());
        return super.getErrorMessage(exception);
    }

    @Override
    public Class<StatusRuntimeException> getExceptionType() {
        return StatusRuntimeException.class;
    }
}

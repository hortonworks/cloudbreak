package com.sequenceiq.cloudbreak.exception.mapper;

import static io.grpc.Status.Code.DEADLINE_EXCEEDED;

import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            case FAILED_PRECONDITION:
                return Response.Status.BAD_REQUEST;
            case PERMISSION_DENIED:
                return Response.Status.FORBIDDEN;
            case UNIMPLEMENTED:
                return Response.Status.NOT_IMPLEMENTED;
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
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
        if (DEADLINE_EXCEEDED.equals(exception.getStatus().getCode())) {
            return "Service Unavailable";
        } else {
            return super.getErrorMessage(exception);
        }
    }

    @Override
    public Class<StatusRuntimeException> getExceptionType() {
        return StatusRuntimeException.class;
    }
}

package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class OperationExceptionMapper implements ExceptionMapper<OperationException> {

    @Override
    public Response toResponse(OperationException exception) {
        Status status = INTERNAL_SERVER_ERROR;
        String message = exception.getMessage();
        int authenticationExceptionIndex = ExceptionUtils.indexOfType(exception, AccessDeniedException.class);
        if (authenticationExceptionIndex != -1) {
            status = FORBIDDEN;
            message = ExceptionUtils.getThrowableList(exception).get(authenticationExceptionIndex).getMessage();
        }
        return Response.status(status).entity(new ExceptionResult(message)).build();
    }
}

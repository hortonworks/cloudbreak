package com.sequenceiq.cloudbreak.controller.mapper;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

@Component
public class OperationExceptionMapper implements TypeAwareExceptionMapper<OperationException> {

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

    @Override
    public Class<OperationException> supportedType() {
        return OperationException.class;
    }
}

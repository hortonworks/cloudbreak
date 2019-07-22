package com.sequenceiq.cloudbreak.common.exception;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public class ClientErrorExceptionHandler {

    private ClientErrorExceptionHandler() { }

    public static String getErrorMessage(ClientErrorException cee) {
        try (Response response = cee.getResponse()) {
            ExceptionResponse result = response.readEntity(ExceptionResponse.class);
            return result.getMessage();
        }
    }
}

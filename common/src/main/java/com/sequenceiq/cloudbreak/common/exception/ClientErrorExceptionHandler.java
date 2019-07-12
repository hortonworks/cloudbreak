package com.sequenceiq.cloudbreak.common.exception;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.common.json.ExceptionResult;

public class ClientErrorExceptionHandler {

    private ClientErrorExceptionHandler() { }

    public static String getErrorMessage(ClientErrorException cee) {
        try (Response response = cee.getResponse()) {
            ExceptionResult result = response.readEntity(ExceptionResult.class);
            return result.getMessage();
        }
    }
}

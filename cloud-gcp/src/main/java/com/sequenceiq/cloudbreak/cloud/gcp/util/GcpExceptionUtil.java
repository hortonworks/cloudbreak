package com.sequenceiq.cloudbreak.cloud.gcp.util;

import org.apache.http.HttpStatus;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

public class GcpExceptionUtil {
    private static final String ERROR_CODE = "code";

    private GcpExceptionUtil() {
    }

    public static boolean resourceNotFoundException(GoogleJsonResponseException ex) {
        return ex.getDetails() != null
                && ex.getDetails().containsKey(ERROR_CODE)
                && (ex.getDetails().get(ERROR_CODE).equals(HttpStatus.SC_NOT_FOUND) || ex.getDetails().get(ERROR_CODE).equals(HttpStatus.SC_FORBIDDEN));
    }
}

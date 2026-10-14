package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpResponseException;

/**
 * A Cloud SQL request rejected with HTTP 409 because another operation is in progress on the instance. It keeps the status,
 * details and message of the original exception (which is also its cause), so callers handle it like any other
 * {@link GoogleJsonResponseException}, while the retry can target this type alone.
 */
public class GcpDatabaseOperationConflictException extends GoogleJsonResponseException {

    public GcpDatabaseOperationConflictException(GoogleJsonResponseException conflict) {
        super(new HttpResponseException.Builder(conflict.getStatusCode(), conflict.getStatusMessage(), conflict.getHeaders())
                .setContent(conflict.getContent())
                .setMessage(conflict.getMessage()), conflict.getDetails());
        initCause(conflict);
    }
}

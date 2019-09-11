package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.InvalidGcpContextException;

@Service
public class GcpCredentialVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialVerifier.class);

    /**
     * Checks both the provided GcpContexts's project id, service account id
     * and compute, to be sure the given context is valid for further
     * operations.
     *
     * @param context the GcpContext which should contain a valid project
     *                id, service account id and compute instance
     */
    public void checkGcpContextValidity(GcpContext context) throws InvalidGcpContextException {
        if (context == null) {
            throw new InvalidGcpContextException("GcpContext has not created properly, it was null");
        } else if (!StringUtils.hasLength(context.getProjectId())) {
            throw new InvalidGcpContextException("Project id is missing.");
        } else if (!StringUtils.hasLength(context.getServiceAccountId())) {
            throw new InvalidGcpContextException("Service account id is missing.");
        } else if (context.getCompute() == null) {
            throw new InvalidGcpContextException("Problem with your credential key please use the correct format.");
        }
    }

    /**
     * Checks the validity of the provided GcpContext's credential. It sends
     * a http request through the google's http api which validates the data
     * set provided from the given GcpContext.
     *
     * @param gcpContext the GcpContext instance which credential would be
     *                   checked.
     *
     * @throws IOException if something happens while listing the regions,
     *                     this exception would thrown by the api.
     */
    @Retryable(
            value = GoogleJsonResponseException.class,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public void preCheckOfGooglePermission(GcpContext gcpContext) throws IOException {
        gcpContext.getCompute().regions().list(gcpContext.getProjectId()).execute();
    }
}

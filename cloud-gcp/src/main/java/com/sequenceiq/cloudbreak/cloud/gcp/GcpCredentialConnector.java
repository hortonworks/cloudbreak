package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.InvalidGcpContextException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

@Service
public class GcpCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialConnector.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Override
    public CloudCredentialStatus verify(@Nonnull AuthenticatedContext authenticatedContext) {
        Objects.requireNonNull(authenticatedContext);
        LOGGER.info("Verify credential: {}", authenticatedContext.getCloudCredential());
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, null, false);
        try {
            checkGcpContextValidity(gcpContext);
            preCheckOfGooglePermission(gcpContext);
        } catch (TokenResponseException te) {
            return createFailedCloudCredentialStatusWithExc(te, authenticatedContext, getErrDescriptionFromTokenResponse(te));
        } catch (Exception e) {
            return createFailedCloudCredentialStatusWithExc(e, authenticatedContext, Optional.empty());
        }
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(@Nonnull AuthenticatedContext authenticatedContext) {
        Objects.requireNonNull(authenticatedContext);
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public Map<String, String> interactiveLogin(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential,
            CredentialNotifier credentialNotifier) {
        throw new UnsupportedOperationException("Interactive login not supported on GCP");
    }

    @Override
    public CloudCredentialStatus delete(@Nonnull AuthenticatedContext authenticatedContext) {
        Objects.requireNonNull(authenticatedContext);
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }

    /**
     * Creates a new CloudCredentialStatus instance with a FAILED status which
     * contains the provided exception and some status reason about the cause
     * of the status.
     *
     * @param e           The provided exception which stores the reason of
     *                    the failed status.
     * @param authContext The AuthenticatedContext instance which will be
     *                    stored in the returning CloudCredentialStatus
     *                    instance.
     * @param message     The custom reason message which also be stored in the
     *                    AuthenticatedContext.
     *                    If the passed Optional is empty, then a default
     *                    message is going to be passed to the status
     *                    instance.
     * @return The combined CloudCredentialStatus instance which stores all
     * the necessary/required data for a proper object with a FAILED status.
     */
    private CloudCredentialStatus createFailedCloudCredentialStatusWithExc(Exception e, AuthenticatedContext authContext, Optional<String> message) {
        LOGGER.warn(String.format("Could not verify credential, detailed message: %s", e.getMessage()), e);
        return new CloudCredentialStatus(authContext.getCloudCredential(), CredentialStatus.FAILED, e, message.orElse("Could not verify credential!"));
    }

    /**
     * Checks the validity of the provided GcpContext's credential. It sends
     * a http request through the google's http api which validates the data
     * set provided from the given GcpContext.
     *
     * @param gcpContext the GcpContext instance which credential would be
     *                   checked.
     * @throws IOException if something happens while listing the regions,
     *                     this exception would thrown by the api.
     */
    private void preCheckOfGooglePermission(GcpContext gcpContext) throws IOException {
        gcpContext.getCompute().regions().list(gcpContext.getProjectId()).executeUsingHead();
    }

    /**
     * Attempts to get the "error_description" parameter's value from the given
     * TokenResponseException's content. If there is no "error_description"
     * parameter, or it has no value or something occurs during the process, an
     * empty Optional would return.
     *
     * @param e The TokenResponseException which content should have a
     *          "error_description" parameter with a string value.
     * @return A String Optional with the content of the "error_description"
     * from the exception, or an empty one.
     */
    private Optional<String> getErrDescriptionFromTokenResponse(TokenResponseException e) {
        try {
            return Optional.of(new JsonParser().parse(e.getContent()).getAsJsonObject().get("error_description").getAsString());
        } catch (RuntimeException re) {
            LOGGER.debug("Could not parse TokenResponseException", re);
            return Optional.empty();
        }
    }

    /**
     * Checks both the provided GcpContexts's project id, service account id
     * and compute, to be sure the given context is valid for further
     * operations.
     *
     * @param context the GcpContext which should contain a valid project
     *                id, service account id and compute instance
     */
    private void checkGcpContextValidity(GcpContext context) throws InvalidGcpContextException {
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

}

package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.GcpCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

@Service
public class GcpCredentialConnector implements CredentialConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCredentialConnector.class);

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Inject
    private GcpCredentialVerifier gcpCredentialVerifier;

    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Override
    public CloudCredentialStatus verify(@Nonnull AuthenticatedContext authenticatedContext) {
        LOGGER.debug("Verify credential: {}", authenticatedContext.getCloudCredential());
        GcpStackUtil.prepareCredential(authenticatedContext.getCloudCredential());
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, null, false);
        try {
            gcpCredentialVerifier.checkGcpContextValidity(gcpContext);
            gcpCredentialVerifier.preCheckOfGooglePermission(gcpContext);
        } catch (TokenResponseException te) {
            return createFailedCloudCredentialStatusWithExc(te, authenticatedContext, getErrDescriptionFromTokenResponse(te));
        } catch (Exception e) {
            return createFailedCloudCredentialStatusWithExc(e, authenticatedContext, Optional.empty());
        }
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.VERIFIED);
    }

    @Override
    public CloudCredentialStatus create(@Nonnull AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(@Nonnull AuthenticatedContext authenticatedContext) {
        return new CloudCredentialStatus(authenticatedContext.getCloudCredential(), CredentialStatus.DELETED);
    }

    @Override
    public CredentialPrerequisitesV4Response getPrerequisites(CloudContext cloudContext, String externalId, String deploymentAddress) {
        String prerequisitesCreationCommand = gcpPlatformParameters.getPrerequisitesCreationCommand();
        GcpCredentialPrerequisites gcpPrereqs = new GcpCredentialPrerequisites(Base64.encodeBase64String(prerequisitesCreationCommand.getBytes()));
        return new CredentialPrerequisitesV4Response(cloudContext.getPlatform().value(), gcpPrereqs);
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
        LOGGER.info(String.format("Could not verify credential, detailed message: %s", e.getMessage()), e);
        return new CloudCredentialStatus(authContext.getCloudCredential(), CredentialStatus.FAILED, e, message.orElse(
                "Could not verify credential! " + e.getMessage()));
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

}

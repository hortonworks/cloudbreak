package com.sequenceiq.environment.environment.dto.credential;

import static com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType.UNKNOWN;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class CredentialDetailsConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialDetailsConverter.class);

    @Inject
    private Map<CloudPlatform, CloudPlatformAwareCredentialDetailsConverter> cloudPlatformAwareCredentialDetailsConverterMap;

    public CredentialDetails credentialToCredentialDetails(CloudPlatform cloudPlatform, Credential credential) {
        CredentialDetails.Builder builder = CredentialDetails.builder();
        return getCredentialAttributes(credential)
                .map(credentialAttributes -> convert(cloudPlatform, builder, credentialAttributes))
                .orElseGet(() -> createFallbackCredentialDetails(builder))
                .build();
    }

    private Optional<CredentialAttributes> getCredentialAttributes(Credential credential) {
        try {
            if (credential.getAttributes() == null) {
                LOGGER.error("Credential attribute is null from Vault. Probably a connection " +
                        "issue or misconfiguration. The affected credential is: {}", credential.getResourceCrn());
                throw new CloudbreakServiceException("Connection issue on CDP control plane please contact with the support.");
            } else {
                return Optional.of(new Json(credential.getAttributes()).get(CredentialAttributes.class));
            }
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private CredentialDetails.Builder convert(CloudPlatform cloudPlatform, CredentialDetails.Builder builder, CredentialAttributes credentialAttributes) {
        if (cloudPlatform != null && cloudPlatformAwareCredentialDetailsConverterMap.containsKey(cloudPlatform)) {
            return cloudPlatformAwareCredentialDetailsConverterMap.get(cloudPlatform).convertCredentialDetails(credentialAttributes, builder);
        } else {
            return createFallbackCredentialDetails(builder);
        }
    }

    private CredentialDetails.Builder createFallbackCredentialDetails(CredentialDetails.Builder builder) {
        LOGGER.debug("CredentialAttributes is null, cloud platform specific credential details cannot be created");
        return builder.withCredentialType(UNKNOWN);
    }
}

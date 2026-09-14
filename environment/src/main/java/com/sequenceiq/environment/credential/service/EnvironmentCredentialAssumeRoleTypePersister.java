package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.credential.CredentialAssumeRoleTypePersister;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

@Service
public class EnvironmentCredentialAssumeRoleTypePersister implements CredentialAssumeRoleTypePersister {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCredentialAssumeRoleTypePersister.class);

    private final CredentialRetrievalService credentialRetrievalService;

    private final CredentialUpdateService credentialUpdateService;

    public EnvironmentCredentialAssumeRoleTypePersister(CredentialRetrievalService credentialRetrievalService,
            CredentialUpdateService credentialUpdateService) {
        this.credentialRetrievalService = credentialRetrievalService;
        this.credentialUpdateService = credentialUpdateService;
    }

    @Override
    public void persistAssumeRoleType(String credentialCrn, String accountId, String assumeRoleType) {
        Optional<Credential> credentialOptional = credentialRetrievalService.getOptionalByCrnForAccountId(credentialCrn, accountId, ENVIRONMENT);
        if (credentialOptional.isEmpty()) {
            LOGGER.info("Credential [{}] not persisted yet (e.g. during create verify), skipping roleAssumeType persist", credentialCrn);
            return;
        }
        try {
            Credential credential = credentialOptional.get();
            CredentialAttributes attributes = new Json(credential.getAttributes()).get(CredentialAttributes.class);
            RoleBasedCredentialAttributes roleBased = Optional.ofNullable(attributes.getAws())
                    .map(AwsCredentialAttributes::getRoleBased)
                    .orElse(null);
            if (roleBased == null) {
                LOGGER.info("No AWS roleBased attributes for credential [{}], skipping roleAssumeType persist", credentialCrn);
                return;
            }
            if (!assumeRoleType.equals(roleBased.getRoleAssumeType())) {
                String previousAttributesSecret = credential.getAttributesSecret();
                roleBased.setRoleAssumeType(assumeRoleType);
                credential.setAttributes(new Json(attributes).getValue());
                credentialUpdateService.updateAttributes(credential, previousAttributesSecret);
                LOGGER.info("Persisted roleAssumeType [{}] for credential [{}]", assumeRoleType, credentialCrn);
            } else {
                LOGGER.info("roleAssumeType [{}] already stored for credential [{}], skipping update", assumeRoleType, credentialCrn);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to persist roleAssumeType [{}] for credential [{}]", assumeRoleType, credentialCrn, e);
        }
    }
}

package com.sequenceiq.environment.credential.v1.converter.aws;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.KeyBasedCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class AwsCredentialV1ParametersToAwsCredentialAttributesConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialV1ParametersToAwsCredentialAttributesConverter.class);

    public AwsCredentialAttributes convert(AwsCredentialParameters source, Optional<Credential> originalCredential) {
        AwsCredentialAttributes response = new AwsCredentialAttributes();
        doIfNotNull(source.getKeyBased(), params -> response.setKeyBased(getKeyBased(params)));
        doIfNotNull(source.getRoleBased(), params -> response.setRoleBased(getRoleBased(params, originalCredential)));
        updateDefaultRegionDuringModify(source, originalCredential, response);
        return response;
    }

    public AwsCredentialParameters convert(AwsCredentialAttributes source) {
        AwsCredentialParameters response = new AwsCredentialParameters();
        doIfNotNull(source.getKeyBased(), params -> response.setKeyBased(getKeyBased(params)));
        doIfNotNull(source.getRoleBased(), params -> response.setRoleBased(getRoleBased(params)));
        doIfNotNull(source.getDefaultRegion(), response::setDefaultRegion);
        return response;
    }

    private RoleBasedCredentialAttributes getRoleBased(RoleBasedParameters source, Optional<Credential> originalCredential) {
        RoleBasedCredentialAttributes roleBased = new RoleBasedCredentialAttributes();
        roleBased.setRoleArn(source.getRoleArn());
        Optional<String> externalId = getExternalId(originalCredential);
        if (externalId.isPresent()) {
            roleBased.setExternalId(externalId.get());
        }
        return roleBased;
    }

    private Optional<String> getExternalId(Optional<Credential> originalCredential) {
        if (originalCredential.isPresent()) {
            Json json = new Json(originalCredential.get().getAttributes());
            try {
                CredentialAttributes credentialAttributes = json.get(CredentialAttributes.class);
                return Optional.ofNullable(credentialAttributes)
                        .map(CredentialAttributes::getAws)
                        .map(AwsCredentialAttributes::getRoleBased)
                        .map(RoleBasedCredentialAttributes::getExternalId);
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private KeyBasedCredentialAttributes getKeyBased(KeyBasedParameters source) {
        KeyBasedCredentialAttributes keyBased = new KeyBasedCredentialAttributes();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }

    private RoleBasedParameters getRoleBased(RoleBasedCredentialAttributes source) {
        RoleBasedParameters roleBased = new RoleBasedParameters();
        roleBased.setRoleArn(source.getRoleArn());
        return roleBased;
    }

    private KeyBasedParameters getKeyBased(KeyBasedCredentialAttributes source) {
        KeyBasedParameters keyBased = new KeyBasedParameters();
        keyBased.setAccessKey(source.getAccessKey());
        keyBased.setSecretKey(source.getSecretKey());
        return keyBased;
    }

    private void updateDefaultRegionDuringModify(AwsCredentialParameters source, Optional<Credential> originalCredential, AwsCredentialAttributes response) {
        if (originalCredential.isPresent()) {
            Credential credential = originalCredential.get();
            if (StringUtils.isNotEmpty(source.getDefaultRegion())) {
                if (StringUtils.isNotEmpty(credential.getAttributes())) {
                    CredentialAttributes attributes = JsonUtil.readValueUnchecked(originalCredential.get().getAttributes(), CredentialAttributes.class);
                    String originalDefaultRegion = Optional.ofNullable(attributes)
                            .map(CredentialAttributes::getAws)
                            .map(AwsCredentialAttributes::getDefaultRegion)
                            .orElse(null);
                    LOGGER.info("Default region changed from: {} to: {}", originalDefaultRegion, source.getDefaultRegion());
                    response.setDefaultRegion(source.getDefaultRegion());
                }
            }
        }
    }
}

package com.sequenceiq.cloudbreak.service.environment.credential;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.dto.credential.aws.AwsCredentialAttributes;
import com.sequenceiq.cloudbreak.dto.credential.aws.AwsKeyBasedAttributes;
import com.sequenceiq.cloudbreak.dto.credential.aws.AwsRoleBasedAttributes;
import com.sequenceiq.cloudbreak.dto.credential.azure.AzureCredentialAttributes;
import com.sequenceiq.cloudbreak.dto.credential.azure.AzureRoleBasedAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.KeyBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@Component
public class CredentialConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialConverter.class);

    @Inject
    private SecretService secretService;

    public Set<Credential> convert(Collection<CredentialResponse> sources) {
        return sources.stream().map(this::convert).collect(Collectors.toSet());
    }

    public Credential convert(CredentialResponse source) {
        Credential.Builder builder = Credential.builder()
                .aws(getIfNotNull(source.getAws(), this::aws))
                .azure(getIfNotNull(source.getAzure(), this::azure))
                .crn(source.getCrn())
                .description(source.getDescription())
                .account(source.getAccountId())
                .creator(source.getCreator())
                .name(source.getName())
                .cloudPlatform(source.getCloudPlatform());
        getAttributesSecret(source, builder);
        return builder.build();

    }

    private AwsCredentialAttributes aws(AwsCredentialParameters aws) {
        return AwsCredentialAttributes.builder()
                .govCloud(aws.getGovCloud())
                .keyBased(getIfNotNull(aws.getKeyBased(), this::keyBased))
                .roleBased(getIfNotNull(aws.getRoleBased(), this::roleBased))
                .build();
    }

    private AzureCredentialAttributes azure(AzureCredentialResponseParameters azure) {
        return AzureCredentialAttributes.builder()
                .accessKey(azure.getAccessKey())
                .roleBased(getIfNotNull(azure.getRoleBased(), this::roleBased))
                .subscriptionId(azure.getSubscriptionId())
                .tenantId(azure.getTenantId())
                .build();
    }

    private AzureRoleBasedAttributes roleBased(RoleBasedResponse roleBased) {
        return AzureRoleBasedAttributes.builder()
                .appObjectId(roleBased.getAppObjectId())
                .codeGrantFlow(roleBased.getCodeGrantFlow())
                .deploymentAddress(roleBased.getDeploymentAddress())
                .spDisplayName(roleBased.getSpDisplayName())
                .build();
    }
    
    private AwsRoleBasedAttributes roleBased(RoleBasedParameters roleBased) {
        return AwsRoleBasedAttributes.builder()
                .roleArn(roleBased.getRoleArn())
                .build();
    }

    private AwsKeyBasedAttributes keyBased(KeyBasedParameters keyBased) {
        return AwsKeyBasedAttributes.builder()
                .accessKey(keyBased.getAccessKey())
                .secretKey(keyBased.getSecretKey())
                .build();
    }

    private void getAttributesSecret(CredentialResponse source, Credential.Builder builder) {
        try {
            builder.attributes(new Json(secretService.getByResponse(source.getAttributes())));
        } catch (VaultException e) {
            String message = String.format("Failed to get Credential config related secret due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}

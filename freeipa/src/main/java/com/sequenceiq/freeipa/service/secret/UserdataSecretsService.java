package com.sequenceiq.freeipa.service.secret;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.SecretConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.UserdataSecretsException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.cloud.model.secret.CloudSecret;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.CreateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.DeleteCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretResourceAccessRequest;
import com.sequenceiq.cloudbreak.cloud.util.UserdataSecretsUtil;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.StackEncryptionService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import software.amazon.awssdk.arns.Arn;

@Component
public class UserdataSecretsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserdataSecretsService.class);

    private static final String SECRET_NAME_FORMAT = "%s-%s-userdata-secret-%s";

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private UserdataSecretsUtil userdataSecretsUtil;

    public List<Resource> createUserdataSecrets(Stack stack, List<Long> privateIds, CloudContext cloudContext, CloudCredential cloudCredential) {
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        SecretConnector secretConnector = cloudConnector.secretConnector();
        LOGGER.info("Creating userdata secrets for instances with privateIds [{}] in stack [{}]...", privateIds, stack.getName());
        CreateCloudSecretRequest.Builder createRequestBuilder = CreateCloudSecretRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withEncryptionKeySource(getEncryptionKeySource(stack, secretConnector))
                .withSecretValue("PLACEHOLDER")
                .withDescription("Created by CDP. This secret stores the sensitive values needed on the instance during first boot.");

        List<String> secretReferences = new ArrayList<>();
        for (Long privateId : privateIds) {
            createRequestBuilder.withSecretName(createSecretName(stack.getName(), stack.getResourceCrn(), privateId));
            CloudSecret cloudSecret = secretConnector.createCloudSecret(createRequestBuilder.build());
            secretReferences.add(cloudSecret.secretId());
        }
        return resourceService.findByResourceReferencesAndStatusAndTypeAndStack(secretReferences, CommonStatus.CREATED, ResourceType.AWS_SECRETSMANAGER_SECRET,
                stack.getId());
    }

    public void assignSecretsToInstances(Stack stack, List<Resource> secretResources, List<InstanceMetaData> instances) {
        if (secretResources.size() != instances.size()) {
            throw new UserdataSecretsException("The number of secrets and number of instances do not match.");
        }
        String stackName = stack.getName();
        String stackCrn = stack.getResourceCrn();
        for (InstanceMetaData instance : instances) {
            Optional<Long> secretId = secretResources.stream()
                    .filter(resource -> resource.getResourceName().equals(createSecretName(stackName, stackCrn, instance.getPrivateId())))
                    .map(Resource::getId)
                    .findFirst();
            LOGGER.info("Assigning userdata secret to instance {}...", instance);
            secretId.ifPresentOrElse(instance::setUserdataSecretResourceId, () -> {
                throw new UserdataSecretsException("Secret resource not found for instance: " + instance);
            });
        }
        instanceMetaDataService.saveAll(instances);
    }

    public void updateUserdataSecrets(Stack stack, List<InstanceMetaData> instances, CredentialResponse credentialResponse,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        SecretConnector secretConnector = cloudConnector.secretConnector();
        String userdata = stack.getImage().getUserdataWrapper();
        Optional<String> userdataSecretsSectionOptional = Optional.of(userdataSecretsUtil.getSecretsSection(userdata));
        List<String> principals = buildPrincipalsList(stack, credentialResponse);
        Map<InstanceMetaData, Optional<Resource>> associations = getAssociations(instances);
        LOGGER.info("Updating the userdata secrets for stack [{}]...", stack.getName());
        UpdateCloudSecretResourceAccessRequest.Builder updatePolicyRequestBuilder = UpdateCloudSecretResourceAccessRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withPrincipals(principals);
        UpdateCloudSecretRequest.Builder updateRequestBuilder = UpdateCloudSecretRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withNewSecretValue(userdataSecretsSectionOptional)
                .withNewEncryptionKeySource(getEncryptionKeySource(stack, secretConnector));

        for (Map.Entry<InstanceMetaData, Optional<Resource>> entry : associations.entrySet()) {
            InstanceMetaData instanceMetaData = entry.getKey();
            Optional<Resource> secretResourceOptional = entry.getValue();
            if (secretResourceOptional.isPresent()) {
                CloudResource cloudResource = resourceToCloudResourceConverter.convert(secretResourceOptional.get());
                updatePolicyRequestBuilder
                        .withCloudResource(cloudResource)
                        .withAuthorizedClients(buildAuthorizedClientsList(stack, instanceMetaData.getInstanceId()));
                secretConnector.updateCloudSecretResourceAccess(updatePolicyRequestBuilder.build());
                updateRequestBuilder
                        .withCloudResource(cloudResource);
                secretConnector.updateCloudSecret(updateRequestBuilder.build());
            } else {
                LOGGER.warn("Instance [{}] has no secret associated with it (the userdataSecretResourceId field is null)!", instanceMetaData.getInstanceId());
            }
        }
    }

    public void deleteUserdataSecretsForInstances(List<InstanceMetaData> instances, CloudContext cloudContext, CloudCredential cloudCredential) {
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        SecretConnector secretConnector = cloudConnector.secretConnector();
        Map<InstanceMetaData, Optional<Resource>> associations = getAssociations(instances);
        for (Map.Entry<InstanceMetaData, Optional<Resource>> entry : associations.entrySet()) {
            InstanceMetaData instanceMetaData = entry.getKey();
            Optional<Resource> secretResource = entry.getValue();
            if (secretResource.isPresent()) {
                LOGGER.info("Removing association between instance [{}] and secret [{}]...", instanceMetaData, secretResource.get().getResourceName());
                instanceMetaData.setUserdataSecretResourceId(null);
            }
        }
        instanceMetaDataService.saveAll(associations.keySet());

        LOGGER.info("Deleting userdata secrets for instances [{}]...", associations.keySet());
        DeleteCloudSecretRequest.Builder deleteRequestBuilder = DeleteCloudSecretRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential);

        for (Map.Entry<InstanceMetaData, Optional<Resource>> entry : associations.entrySet()) {
            InstanceMetaData instanceMetaData = entry.getKey();
            Optional<Resource> secretResource = entry.getValue();
            if (secretResource.isPresent()) {
                CloudResource cloudResource = resourceToCloudResourceConverter.convert(secretResource.get());
                LOGGER.info("Deleting secret resource [{}] formerly associated with instance [{}]...", secretResource.get().getResourceName(),
                        instanceMetaData.getInstanceId());
                deleteRequestBuilder
                        .withSecretName(cloudResource.getName())
                        .withCloudResources(List.of(cloudResource));
                secretConnector.deleteCloudSecret(deleteRequestBuilder.build());
            }
        }
    }

    private static String createSecretName(String stackName, String stackCrn, Long instancePrivateId) {
        // AWS Secrets Manager only allows the following characters in secret names: alphanumeric characters and the these [-/_+=.@!] special characters
        String modifiedStackCrn = stackCrn.replace(':', '-');
        return String.format(SECRET_NAME_FORMAT, stackName, modifiedStackCrn, instancePrivateId);
    }

    private Map<InstanceMetaData, Optional<Resource>> getAssociations(List<InstanceMetaData> instances) {
        List<Long> secretResourceIds = instances.stream()
                .filter(Objects::nonNull)
                .map(InstanceMetaData::getUserdataSecretResourceId)
                .toList();
        Iterable<Resource> secretResources = resourceService.findAllByResourceId(secretResourceIds);
        Map<InstanceMetaData, Optional<Resource>> associations = new HashMap<>(instances.size());
        instances.forEach(imd -> {
            Optional<Resource> secretResource = StreamSupport.stream(secretResources.spliterator(), false)
                    .filter(r -> r.getId().equals(imd.getUserdataSecretResourceId()))
                    .findFirst();
            associations.put(imd, secretResource);
        });
        return associations;
    }

    private Optional<EncryptionKeySource> getEncryptionKeySource(Stack stack, SecretConnector secretConnector) {
        StackEncryption stackEncryption = stackEncryptionService.getStackEncryption(stack.getId());
        if (stackEncryption.getEncryptionKeyCloudSecretManager() != null) {
            return Optional.of(EncryptionKeySource.builder()
                    .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                    .withKeyValue(stackEncryption.getEncryptionKeyCloudSecretManager())
                    .build());
        } else {
            return Optional.of(secretConnector.getDefaultEncryptionKeySource());
        }
    }

    private static List<String> buildPrincipalsList(Stack stack, CredentialResponse credentialResponse) {
        return switch (stack.getCloudPlatform()) {
            case "AWS" -> {
                String instanceProfileArn = stack.getTelemetry().getLogging().getS3().getInstanceProfile();
                String crossAccountRoleArn = credentialResponse.getAws().getRoleBased().getRoleArn();
                yield List.of(instanceProfileArn, crossAccountRoleArn);
            }
            default -> Collections.emptyList();
        };
    }

    private static List<String> buildAuthorizedClientsList(Stack stack, String instanceId) {
        return switch (stack.getCloudPlatform()) {
            case "AWS" -> {
                String instanceProfileArn = stack.getTelemetry().getLogging().getS3().getInstanceProfile();
                Arn arn = Arn.fromString(instanceProfileArn);
                if (arn.accountId().isPresent()) {
                    yield List.of(buildAwsEc2InstanceArn(arn.partition(), stack.getRegion(), arn.accountId().get(), instanceId));
                } else {
                    yield Collections.emptyList();
                }
            }
            default -> Collections.emptyList();
        };
    }

    private static String buildAwsEc2InstanceArn(String partition, String region, String accountId, String instanceId) {
        return Arn.builder()
                .partition(partition)
                .service("ec2")
                .region(region)
                .accountId(accountId)
                .resource("instance/" + instanceId)
                .build()
                .toString();
    }
}

package com.sequenceiq.cloudbreak.service.encryption;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.SecretConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.UserdataSecretsException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.secret.CloudSecret;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.CreateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.DeleteCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretResourceAccessRequest;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.util.UserdataSecretsUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackEncryptionService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class UserdataSecretsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserdataSecretsService.class);

    private static final String SECRET_NAME_FORMAT = "%s-%s-userdata-secret-%s";

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private UserDataService userDataService;

    @Inject
    private StackEncryptionService stackEncryptionService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private UserdataSecretsUtil userdataSecretsUtil;

    @Inject
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    public List<Resource> createUserdataSecrets(Stack stack, List<Long> privateIds, CloudContext cloudContext, CloudCredential cloudCredential) {
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        SecretConnector secretConnector = cloudConnector.secretConnector();
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        LOGGER.info("Creating userdata secrets for instances with privateIds [{}] in stack [{}]...", privateIds, stack.getName());
        CreateCloudSecretRequest.Builder createRequestBuilder = CreateCloudSecretRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withEncryptionKeySource(Optional.of(secretConnector.getDefaultEncryptionKeySource()))
                .withSecretValue("PLACEHOLDER")
                .withDescription("Created by CDP. This secret stores the sensitive values needed on the instance during first boot.")
                .withTags(getTags(stack));

        List<String> secretReferences = new ArrayList<>();
        for (Long privateId : privateIds) {
            createRequestBuilder.withSecretName(createSecretName(stack.getName(), stack.getResourceCrn(), privateId));
            CloudSecret cloudSecret = secretConnector.createCloudSecret(createRequestBuilder.build());
            secretReferences.add(cloudSecret.secretId());
        }
        ResourceType resourceType = cloudInformationDecorator.getUserdataSecretResourceType();
        return resourceService.findByResourceReferencesAndStatusAndTypeAndStack(secretReferences, CommonStatus.CREATED, resourceType, stack.getId());
    }

    private Map<String, String> getTags(Stack stack) {
        Optional<Json> stackTagsJson = Optional.ofNullable(stack.getTags());
        try {
            Map<String, String> tags = new HashMap<>();
            if (stackTagsJson.isPresent()) {
                StackTags stackTags = stackTagsJson.get().get(StackTags.class);
                tags.putAll(stackTags.getDefaultTags());
                tags.putAll(stackTags.getApplicationTags());
            }
            return tags;
        } catch (IOException e) {
            throw new UserdataSecretsException("Failed to prepare tags for userdata secrets.", e);
        }
    }

    private Optional<EncryptionKeySource> getEncryptionKeySource(Stack stack, SecretConnector secretConnector) {
        StackEncryption stackEncryption = stackEncryptionService.getStackEncryption(stack.getId());
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        if (stackEncryption.getEncryptionKeyCloudSecretManager() != null) {
            return Optional.of(EncryptionKeySource.builder()
                    .withKeyType(cloudInformationDecorator.getUserdataSecretEncryptionKeyType())
                    .withKeyValue(stackEncryption.getEncryptionKeyCloudSecretManager())
                    .build());
        } else {
            LOGGER.warn("Cloud Secret Manager key not found for stack. Using the default key.");
            return Optional.of(secretConnector.getDefaultEncryptionKeySource());
        }
    }

    private static String createSecretName(String stackName, String stackCrn, Long instancePrivateId) {
        // AWS Secrets Manager only allows the following characters in secret names: alphanumeric characters and the these [-/_+=.@!] special characters
        String modifiedStackCrn = stackCrn.replace(':', '-');
        return String.format(SECRET_NAME_FORMAT, stackName, modifiedStackCrn, instancePrivateId);
    }

    public void assignSecretsToInstances(StackDtoDelegate stack, List<Resource> secretResources, List<InstanceMetaData> instances) {
        if (secretResources.size() != instances.size()) {
            throw new UserdataSecretsException("The number of secrets and number of instances do not match.");
        }
        String stackName = stack.getName();
        String stackCrn = stack.getResourceCrn();
        for (InstanceMetaData instance : instances) {
            String secretName = createSecretName(stackName, stackCrn, instance.getPrivateId());
            Optional<Long> secretId = secretResources.stream()
                    .filter(resource -> resource.getResourceName().equals(secretName))
                    .map(Resource::getId)
                    .findFirst();
            LOGGER.info("Assigning userdata secret to instance {}...", instance);
            secretId.ifPresentOrElse(instance::setUserdataSecretResourceId, () -> {
                throw new UserdataSecretsException(String.format("Secret resource under name '%s', not found for instance: %s", secretName, instance));
            });
        }
        instanceMetaDataService.saveAll(instances);
    }

    public void updateUserdataSecrets(Stack stack, List<InstanceMetaData> instances, DetailedEnvironmentResponse environment,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        SecretConnector secretConnector = cloudConnector.secretConnector();
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        Map<InstanceGroupType, String> userdatas = userDataService.getUserData(stack.getId());
        Map<InstanceMetaData, Optional<Resource>> associations = getAssociations(instances);
        Map<String, List<String>> cryptographicPrincipalsPerInstanceGroupName =
                cloudInformationDecorator.getUserdataSecretCryptographicPrincipalsForInstanceGroups(environment, stack);
        LOGGER.info("Updating the userdata secrets for stack [{}]...", stack.getName());
        UpdateCloudSecretResourceAccessRequest.Builder updatePolicyRequestBuilder = UpdateCloudSecretResourceAccessRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential);
        UpdateCloudSecretRequest.Builder updateRequestBuilder = UpdateCloudSecretRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withNewEncryptionKeySource(getEncryptionKeySource(stack, secretConnector));

        for (Map.Entry<InstanceMetaData, Optional<Resource>> entry : associations.entrySet()) {
            InstanceMetaData instanceMetaData = entry.getKey();
            Optional<Resource> secretResourceOptional = entry.getValue();
            String userdata = userdatas.get(instanceMetaData.getInstanceGroupType());
            if (secretResourceOptional.isPresent()) {
                CloudResource cloudResource = resourceToCloudResourceConverter.convert(secretResourceOptional.get());
                updatePolicyRequestBuilder
                        .withCloudResource(cloudResource)
                        .withCryptographicPrincipals(cryptographicPrincipalsPerInstanceGroupName.get(instanceMetaData.getInstanceGroupName()))
                        .withCryptographicAuthorizedClients(cloudInformationDecorator.getUserdataSecretCryptographicAuthorizedClients(stack,
                                instanceMetaData.getInstanceId()));
                secretConnector.updateCloudSecretResourceAccess(updatePolicyRequestBuilder.build());
                updateRequestBuilder
                        .withCloudResource(cloudResource)
                        .withNewSecretValue(Optional.of(userdataSecretsUtil.getSecretsSection(userdata)));
                secretConnector.updateCloudSecret(updateRequestBuilder.build());
            } else {
                LOGGER.warn("Instance [{}] has no secret associated with it (the userdataSecretResourceId field is null)!", instanceMetaData.getInstanceId());
            }
        }
    }

    private Map<InstanceMetaData, Optional<Resource>> getAssociations(List<InstanceMetaData> instances) {
        List<InstanceMetaData> instancesWithUserdataSecret = instances.stream()
                .filter(imd -> imd.getUserdataSecretResourceId() != null)
                .toList();
        List<Long> secretResourceIds = instancesWithUserdataSecret.stream()
                .map(InstanceMetaData::getUserdataSecretResourceId)
                .toList();
        List<Resource> secretResources = StreamSupport.stream(resourceService.findAllByResourceId(secretResourceIds).spliterator(), false).toList();
        Map<InstanceMetaData, Optional<Resource>> associations = new HashMap<>(instancesWithUserdataSecret.size());
        instancesWithUserdataSecret.forEach(imd -> {
            Optional<Resource> secretResource = secretResources.stream()
                    .filter(r -> r.getId().equals(imd.getUserdataSecretResourceId()))
                    .findFirst();
            associations.put(imd, secretResource);
        });
        return associations;
    }

    public void deleteUserdataSecretsForStack(Stack stack, CloudContext cloudContext, CloudCredential cloudCredential) {
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        SecretConnector secretConnector = cloudConnector.secretConnector();
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        List<InstanceMetaData> instances = stack.getTerminatedAndNonTerminatedInstanceMetaDataAsList();
        LOGGER.info("Removing all associations between instances and userdata secrets for stack [{}]...", stack.getName());
        instances.forEach(imd -> imd.setUserdataSecretResourceId(null));
        instanceMetaDataService.saveAll(instances);

        ResourceType resourceType = cloudInformationDecorator.getUserdataSecretResourceType();
        List<CloudResource> secretCloudResources = resourceRetriever.findAllByStatusAndTypeAndStack(CommonStatus.CREATED, resourceType, stack.getId());
        DeleteCloudSecretRequest.Builder deleteRequestBuilder = DeleteCloudSecretRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential);
        for (CloudResource cloudResource : secretCloudResources) {
            LOGGER.info("Deleting secret resource [{}]...", cloudResource.getName());
            deleteRequestBuilder
                    .withSecretName(cloudResource.getName())
                    .withCloudResources(List.of(cloudResource));
            secretConnector.deleteCloudSecret(deleteRequestBuilder.build());
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
}

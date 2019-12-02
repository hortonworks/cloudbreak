package com.sequenceiq.datalake.service.validation.cloudstorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidator.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final SecretService secretService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    public CloudStorageValidator(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            SecretService secretService, CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.secretService = secretService;
        this.cloudProviderServicesV4Endpoint = cloudProviderServicesV4Endpoint;
    }

    public void validate(CloudStorageRequest cloudStorageRequest, DetailedEnvironmentResponse environment) {
        LOGGER.info("Validating cloudStorageRequest: {}", JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
        if (cloudStorageRequest != null) {
            Credential credential = getCredential(environment);
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

            ObjectStorageValidateRequest request = createObjectStorageValidateRequest(
                    environment.getCloudPlatform(), cloudCredential, cloudStorageRequest);
            ObjectStorageValidateResponse response = cloudProviderServicesV4Endpoint
                    .validateObjectStorage(request);

            LOGGER.info("ValidateObjectStorage: request: {}, response: {}", JsonUtil.writeValueAsStringSilent(request),
                    JsonUtil.writeValueAsStringSilent(response));

            if (ResponseStatus.ERROR.equals(response.getStatus())) {
                throw new BadRequestException(response.getError());
            }
        }
    }

    private ObjectStorageValidateRequest createObjectStorageValidateRequest(
            String cloudPlatform, CloudCredential credential, CloudStorageRequest cloudStorageRequest) {
        return ObjectStorageValidateRequest.builder()
                .withCloudPlatform(cloudPlatform)
                .withCredential(credential)
                .withCloudStorageRequest(cloudStorageRequest)
                .build();
    }

    private Credential getCredential(DetailedEnvironmentResponse environment) {
        return new Credential(environment.getCloudPlatform(),
                environment.getCredential().getName(),
                secretService.getByResponse(environment.getCredential().getAttributes()),
                environment.getCredential().getCrn());
    }
}

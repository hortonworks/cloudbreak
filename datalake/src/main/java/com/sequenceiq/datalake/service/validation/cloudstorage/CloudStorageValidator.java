package com.sequenceiq.datalake.service.validation.cloudstorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageValidator.class);

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    private final EntitlementService entitlementService;

    private final SecretService secretService;

    private final CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

    public CloudStorageValidator(CredentialToCloudCredentialConverter credentialToCloudCredentialConverter,
            EntitlementService entitlementService, SecretService secretService,
            CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint) {
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
        this.entitlementService = entitlementService;
        this.secretService = secretService;
        this.cloudProviderServicesV4Endpoint = cloudProviderServicesV4Endpoint;
    }

    public void validate(CloudStorageRequest cloudStorageRequest, DetailedEnvironmentResponse environment,
            ValidationResult.ValidationResultBuilder validationResultBuilder) {
        if (CloudStorageValidation.DISABLED.equals(environment.getCloudStorageValidation())) {
            LOGGER.info("Due to cloud storage validation not being enabled, not validating cloudStorageRequest: {}",
                    JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
            return;
        }

        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.cloudStorageValidationEnabled(accountId)) {
            LOGGER.info("Cloud storage validation entitlement is missing, not validating cloudStorageRequest: {}",
                    JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
            return;
        }

        LOGGER.info("Validating cloudStorageRequest: {}", JsonUtil.writeValueAsStringSilent(cloudStorageRequest));
        if (cloudStorageRequest != null) {
            Credential credential = getCredential(environment);
            CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

            ObjectStorageValidateRequest request = createObjectStorageValidateRequest(
                    environment.getCloudPlatform(), cloudCredential, cloudStorageRequest);
            ObjectStorageValidateResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    cloudProviderServicesV4Endpoint.validateObjectStorage(request));

            LOGGER.info("ValidateObjectStorage: request: {}, response: {}", JsonUtil.writeValueAsStringSilent(request),
                    JsonUtil.writeValueAsStringSilent(response));

            if (ResponseStatus.ERROR.equals(response.getStatus())) {
                validationResultBuilder.error(response.getError());
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

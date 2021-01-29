package com.sequenceiq.datalake.service.validation.cloudstorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
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
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.datalake.entity.Credential;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class CloudStorageValidator {
    // this is hard limit from AWS that EFS mount target can only be associated with at most 5 security groups
    // when a mount target of an EFS instance is configured to be associated with more than 5 security groups, the provision fails
    public static final int EFS_SECURITYGROUP_COUNT_MAX = 5;

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

    public void validate(StackV4Request stackV4Request, DetailedEnvironmentResponse environment,
            ValidationResult.ValidationResultBuilder validationResultBuilder) {
        CloudStorageRequest cloudStorageRequest = stackV4Request.getCluster().getCloudStorage();

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

            validateEfsParameters(stackV4Request, validationResultBuilder);
        }
    }

    private void validateEfsParameters(StackV4Request stackV4Request, ValidationResult.ValidationResultBuilder validationResultBuilder) {
        CloudStorageRequest cloudStorageRequest = stackV4Request.getCluster().getCloudStorage();
        AwsEfsParameters efsParameters = cloudStorageRequest.getAws() == null ? null : cloudStorageRequest.getAws().getEfsParameters();

        if (efsParameters == null) {
            LOGGER.info("validateEfsParameters: there is no efs parameters configured.");
            return;
        }

        if (StringUtils.isEmpty(efsParameters.getName())) {
            String errorMessage = "EFS parameter is present in cloud storage, but its name is not configured.";
            validationResultBuilder.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }

        if (efsParameters.getAssociatedInstanceGroupNames() == null || efsParameters.getAssociatedInstanceGroupNames().size() == 0) {
            String errorMessage = "EFS parameter is present in cloud storage, but its associated instance group names are not configured.";
            validationResultBuilder.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }

        List<String> associatedInstanceGroupNames = efsParameters.getAssociatedInstanceGroupNames();
        List<InstanceGroupV4Request> instanceGroups = stackV4Request.getInstanceGroups();
        int totalSecurityGroupCount = getTotalAssociatedSecurityGroupCount(associatedInstanceGroupNames, instanceGroups);

        if (totalSecurityGroupCount < 1 || totalSecurityGroupCount > EFS_SECURITYGROUP_COUNT_MAX) {
            String errorMessage = String.format(
                    "The associated security groups of AWS EFS has to be larger than 0 and less than or equal to %s. However, it is configured to be %s",
                    EFS_SECURITYGROUP_COUNT_MAX, totalSecurityGroupCount);
            validationResultBuilder.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    private int getTotalAssociatedSecurityGroupCount(List<String> associatedInstanceGroupNames, List<InstanceGroupV4Request> instanceGroups) {
        int totalSecurityGroupCount = 0;
        Set<String> associatedSecurityGroupIds = new HashSet<>();
        for (InstanceGroupV4Request groupV4Request : instanceGroups) {
            SecurityGroupV4Request securityGroup = groupV4Request.getSecurityGroup();
            if (securityGroup.getSecurityGroupIds() != null && associatedInstanceGroupNames.contains(groupV4Request.getName())) {
                for (String securityGroupId : securityGroup.getSecurityGroupIds()) {
                    if (!associatedSecurityGroupIds.contains(securityGroupId)) {
                        totalSecurityGroupCount++;
                        associatedSecurityGroupIds.add(securityGroupId);
                    }
                }
            }

            if (securityGroup.getSecurityRules() != null && associatedInstanceGroupNames.contains(groupV4Request.getName())) {
                totalSecurityGroupCount += securityGroup.getSecurityRules().size();
            }
        }

        return totalSecurityGroupCount;
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

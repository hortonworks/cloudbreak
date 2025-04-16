package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.model.CredentialStatus.PERMISSIONS_MISSING;
import static com.sequenceiq.cloudbreak.cloud.response.PolicyComponentIdentifier.DATALAKE;
import static com.sequenceiq.cloudbreak.cloud.response.PolicyComponentIdentifier.ENVIRONMENT;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsConfusedDeputyException;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse;
import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponses;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.credential.CredentialVerificationContext;
import com.sequenceiq.cloudbreak.cloud.response.AwsCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.cloud.response.GranularPolicyResponse;
import com.sequenceiq.cloudbreak.experience.PolicyServiceName;
import com.sequenceiq.common.model.CredentialType;

import software.amazon.awssdk.core.exception.SdkException;

@Service
public class AwsCredentialConnector implements CredentialConnector {

    static final String ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR = "is not authorized to perform: sts:AssumeRole on resource";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialConnector.class);

    @Value("${cb.aws.account.id:}")
    private String accountId;

    @Value("${cb.aws.gov.account.id:}")
    private String govAccountId;

    @Inject
    private AwsSessionCredentialClient credentialClient;

    @Inject
    private AwsCredentialVerifier awsCredentialVerifier;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsCredentialViewProvider credentialViewProvider;

    @Inject
    private AwsDefaultRegionSelector defaultRegionSelector;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public CloudCredentialStatus verify(AuthenticatedContext authenticatedContext, CredentialVerificationContext credentialVerificationContext) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        LOGGER.debug("Create credential: {}", credential);
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(credential);
        String roleArn = awsCredential.getRoleArn();
        String accessKey = awsCredential.getAccessKey();
        String secretKey = awsCredential.getSecretKey();

        CloudCredentialStatus result;
        if (isNoneEmpty(roleArn, accessKey, secretKey)) {
            String message = "Please only provide the 'role arn' or the 'access' and 'secret key'";
            result = new CloudCredentialStatus(credential, CredentialStatus.FAILED, new Exception(message), message);
        } else if (isNotEmpty(roleArn)) {
            result = verifyIamRoleIsAssumable(credential, credentialVerificationContext);
        } else if (isEmpty(accessKey) || isEmpty(secretKey)) {
            String message = "Please provide both the 'access' and 'secret key'";
            result = new CloudCredentialStatus(credential, CredentialStatus.FAILED, new Exception(message), message);
        } else {
            result = verifyAccessKeySecretKeyIsAssumable(credential);
        }
        return result;
    }

    @Override
    public CDPServicePolicyVerificationResponses verifyByServices(AuthenticatedContext authenticatedContext,
            List<String> services, Map<String, String> experiencePrerequisites) {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        LOGGER.debug("Create credential: {}", credential);
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(credential);
        String roleArn = awsCredential.getRoleArn();
        String accessKey = awsCredential.getAccessKey();
        String secretKey = awsCredential.getSecretKey();

        CDPServicePolicyVerificationResponses result;
        if (isNoneEmpty(roleArn, accessKey, secretKey)) {
            String message = "Please only provide the 'role arn' or the 'access' and 'secret key'";
            result = new CDPServicePolicyVerificationResponses(getServiceStatus(services, message));
        } else if (isNotEmpty(roleArn)) {
            result = verifyIamRoleIsAssumable(credential, services, experiencePrerequisites);
        } else if (isEmpty(accessKey) || isEmpty(secretKey)) {
            String message = "Please provide both the 'access' and 'secret key'";
            result = new CDPServicePolicyVerificationResponses(getServiceStatus(services, message));
        } else {
            String message = "We do not support to verify 'access' and 'secret key'";
            result = new CDPServicePolicyVerificationResponses(getServiceStatus(services, message));
        }
        return result;
    }

    @Override
    public CloudCredentialStatus create(AuthenticatedContext auth) {
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.CREATED);
    }

    @Override
    public CloudCredentialStatus delete(AuthenticatedContext auth) {
        return new CloudCredentialStatus(auth.getCloudCredential(), CredentialStatus.DELETED);
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisites(CloudContext cloudContext, String externalId,
        String auditExternalId, String deploymentAddress, CredentialType type) {
        String policyJson;
        boolean govCloud = cloudContext.isGovCloud();
        String actualExternalId = switch (type) {
            case ENVIRONMENT -> {
                policyJson = awsPlatformParameters.getCredentialPoliciesJson().get(getPolicyType(govCloud));
                yield externalId;
            }
            case AUDIT -> {
                policyJson = awsPlatformParameters.getAuditPoliciesJson().get(getPolicyType(govCloud));
                yield auditExternalId;
            }
            default -> {
                LOGGER.debug("Unrecognized credential type: {}", type);
                policyJson = null;
                yield null;
            }
        };
        AwsCredentialPrerequisites awsPrerequisites = new AwsCredentialPrerequisites(
                actualExternalId,
                policyJson,
                new HashMap<>(),
                new HashSet<>());
        awsPrerequisites.setPolicies(collectNecessaryPolicies(govCloud));
        awsPrerequisites.setGranularPolicies(collectNecessaryGranularPolicies(govCloud));
        return new CredentialPrerequisitesResponse(cloudContext.getPlatform().value(), getAccountId(govCloud), awsPrerequisites);
    }

    private PolicyType getPolicyType(boolean govCloud) {
        return govCloud ? PolicyType.GOV : PolicyType.PUBLIC;
    }

    private String getAccountId(boolean govCloud) {
        if (govCloud) {
            return govAccountId;
        } else {
            return accountId;
        }
    }

    @Deprecated
    private Map<String, String> collectNecessaryPolicies(boolean govCloud) {
        return ImmutableMap.<String, String>builder()
                .put("Audit", awsPlatformParameters.getAuditPoliciesJson().get(getPolicyType(govCloud)))
                .put("DynamoDB", awsPlatformParameters.getCdpDynamoDbPolicyJson().get(getPolicyType(govCloud)))
                .put("Bucket_Access", awsPlatformParameters.getCdpBucketAccessPolicyJson().get(getPolicyType(govCloud)))
                .put("Environment", awsPlatformParameters.getEnvironmentMinimalPoliciesJson().get(getPolicyType(govCloud)))
                .put("Ranger_Audit", awsPlatformParameters.getCdpRangerAuditS3PolicyJson().get(getPolicyType(govCloud)))
                .put("Ranger_Raz", awsPlatformParameters.getCdpRangerRazS3PolicyJson().get(getPolicyType(govCloud)))
                .put("Datalake_Admin", awsPlatformParameters.getCdpDatalakeAdminS3PolicyJson().get(getPolicyType(govCloud)))
                .put("Datalake_Backup", awsPlatformParameters.getCdpDatalakeBackupPolicyJson().get(getPolicyType(govCloud)))
                .put("Idbroker_Assumer", awsPlatformParameters.getCdpIdbrokerPolicyJson().get(getPolicyType(govCloud)))
                .put("Datalake_Restore", awsPlatformParameters.getCdpDatalakeRestorePolicyJson().get(getPolicyType(govCloud)))
                .put("Log_Policy", awsPlatformParameters.getCdpLogPolicyJson().get(getPolicyType(govCloud)))
                .build();
    }

    private Set<GranularPolicyResponse> collectNecessaryGranularPolicies(boolean govCloud) {
        Set<GranularPolicyResponse> policies = new HashSet<>();
        policies.addAll(Set.of(
                new GranularPolicyResponse(ENVIRONMENT.name(), "Audit",
                        awsPlatformParameters.getAuditPoliciesJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(ENVIRONMENT.name(), "Idbroker_Assumer",
                        awsPlatformParameters.getCdpIdbrokerPolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(ENVIRONMENT.name(), "Environment",
                        awsPlatformParameters.getEnvironmentMinimalPoliciesJson().get(getPolicyType(govCloud)))
                ));
        policies.addAll(Set.of(
                new GranularPolicyResponse(DATALAKE.name(), "Ranger_Audit",
                        awsPlatformParameters.getCdpRangerAuditS3PolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(DATALAKE.name(), "Ranger_Raz",
                        awsPlatformParameters.getCdpRangerRazS3PolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(DATALAKE.name(), "Bucket_Access",
                        awsPlatformParameters.getCdpBucketAccessPolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(DATALAKE.name(), "Datalake_Admin",
                        awsPlatformParameters.getCdpDatalakeAdminS3PolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(DATALAKE.name(), "Datalake_Backup",
                        awsPlatformParameters.getCdpDatalakeBackupPolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(DATALAKE.name(), "Datalake_Restore",
                        awsPlatformParameters.getCdpDatalakeRestorePolicyJson().get(getPolicyType(govCloud))),
                new GranularPolicyResponse(DATALAKE.name(), "Log_Policy",
                        awsPlatformParameters.getCdpLogPolicyJson().get(getPolicyType(govCloud)))
                ));
        return policies;
    }

    private CDPServicePolicyVerificationResponses verifyIamRoleIsAssumable(CloudCredential cloudCredential,
            List<String> services, Map<String, String> experiencePrerequisites) {
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(cloudCredential);
        CDPServicePolicyVerificationResponses credentialStatus;
        Map<String, String> servicesWithPolicies = new HashMap<>();
        services.forEach(service -> experiencePrerequisites.keySet()
                .stream()
                .filter(AwsCredentialConnector::isPolicyServiceMatchesForName)
                .findFirst()
                .ifPresent(policyKey -> servicesWithPolicies.put(service, experiencePrerequisites.get(policyKey))));
        try {
            credentialClient.retrieveSessionCredentials(awsCredential);
            credentialStatus = verifyCredentialsPermission(awsCredential, servicesWithPolicies);
        } catch (SdkException ae) {
            String errorMessage = getErrorMessageForAwsClientException(awsCredential, ae);
            LOGGER.warn(errorMessage, ae);
            credentialStatus = new CDPServicePolicyVerificationResponses(getServiceStatus(services, errorMessage));
        } catch (AwsConfusedDeputyException confusedDeputyEx) {
            credentialStatus = new CDPServicePolicyVerificationResponses(getServiceStatus(services, confusedDeputyEx.getMessage()));
        } catch (RuntimeException e) {
            String errorMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct external ID. " +
                    "Cause: '%s'", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.warn(errorMessage, e);
            credentialStatus = new CDPServicePolicyVerificationResponses(getServiceStatus(services, errorMessage));
        }
        return credentialStatus;
    }

    private static boolean isPolicyServiceMatchesForName(String policyName) {
        LOGGER.debug("Looking for policy service name alternatives for the given name: {}", policyName);
        for (PolicyServiceName policyServiceName : PolicyServiceName.values()) {
            if (policyName.equalsIgnoreCase(policyServiceName.getPublicName()) || policyServiceName.hasMatchForInternalAlternativesWithIgnoreCase(policyName)) {
                return true;
            }
        }
        return false;
    }

    private Set<CDPServicePolicyVerificationResponse> getServiceStatus(List<String> services, String errorMessage) {
        Set<CDPServicePolicyVerificationResponse> cdpServicePolicyVerificationResponses = new HashSet<>();
        for (String service : services) {
            CDPServicePolicyVerificationResponse cdpServicePolicyVerificationResponse = new CDPServicePolicyVerificationResponse();
            cdpServicePolicyVerificationResponse.setStatusCode(CDPServicePolicyVerificationResponse.SERVICE_UNAVAILABLE);
            cdpServicePolicyVerificationResponse.setServiceStatus(errorMessage);
            cdpServicePolicyVerificationResponse.setServiceName(service);
            cdpServicePolicyVerificationResponses.add(cdpServicePolicyVerificationResponse);
        }
        return cdpServicePolicyVerificationResponses;
    }

    private CloudCredentialStatus verifyIamRoleIsAssumable(CloudCredential cloudCredential, CredentialVerificationContext credentialVerificationContext) {
        AwsCredentialView awsCredential = credentialViewProvider.createAwsCredentialView(cloudCredential);
        CloudCredentialStatus credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.VERIFIED);
        try {
            credentialClient.retrieveSessionCredentials(awsCredential);
            checkRoleIsAssumableWithoutExternalId(credentialVerificationContext, awsCredential, cloudCredential.getAccountId());
            credentialStatus = verifyCredentialsPermission(cloudCredential, awsCredential, credentialStatus);
            credentialStatus = determineDefaultRegion(cloudCredential, credentialStatus);
        } catch (SdkException ae) {
            String errorMessage = getErrorMessageForAwsClientException(awsCredential, ae);
            LOGGER.warn(errorMessage, ae);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, ae, errorMessage);
        } catch (AwsConfusedDeputyException confusedDeputyEx) {
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, confusedDeputyEx, confusedDeputyEx.getMessage());
        } catch (RuntimeException e) {
            String errorMessage = String.format("Unable to verify credential: check if the role '%s' exists and it's created with the correct external ID. " +
                    "Cause: '%s'", awsCredential.getRoleArn(), e.getMessage());
            LOGGER.warn(errorMessage, e);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return credentialStatus;
    }

    private CloudCredentialStatus verifyCredentialsPermission(CloudCredential cloudCredential, AwsCredentialView awsCredential,
            CloudCredentialStatus credentialStatus) {
        if (cloudCredential.getCredentialSettings().isVerifyPermissions()) {
            try {
                String environmentMinimalPoliciesJson = awsPlatformParameters.getEnvironmentMinimalPoliciesJson()
                        .get(getPolicyType(new AwsCredentialView(cloudCredential).isGovernmentCloudEnabled()));
                verifyCredentialsPermission(awsCredential, environmentMinimalPoliciesJson);
            } catch (AwsPermissionMissingException e) {
                credentialStatus = new CloudCredentialStatus(cloudCredential, PERMISSIONS_MISSING, new Exception(e.getMessage()), e.getMessage());
            }
        }
        return credentialStatus;
    }

    private CDPServicePolicyVerificationResponses verifyCredentialsPermission(AwsCredentialView awsCredential,
            Map<String, String> servicesWithPolicies) {
        Set<CDPServicePolicyVerificationResponse> cdpServicePolicyVerificationResponses = new HashSet<>();
        for (Map.Entry<String, String> entry : servicesWithPolicies.entrySet()) {
            String serviceName = entry.getKey();
            String policy = entry.getValue();
            try {
                if (Strings.isNullOrEmpty(policy)) {
                    CDPServicePolicyVerificationResponse cdpServicePolicyVerificationResponse = new CDPServicePolicyVerificationResponse();
                    cdpServicePolicyVerificationResponse.setStatusCode(CDPServicePolicyVerificationResponse.NOT_IMPLEMENTED);
                    cdpServicePolicyVerificationResponse.setServiceStatus("The policy query endpoint was not implement on experience side.");
                    cdpServicePolicyVerificationResponse.setServiceName(serviceName);
                    cdpServicePolicyVerificationResponses.add(cdpServicePolicyVerificationResponse);
                } else {
                    verifyCredentialsPermission(awsCredential, policy);
                }
            } catch (AwsPermissionMissingException e) {
                CDPServicePolicyVerificationResponse cdpServicePolicyVerificationResponse = new CDPServicePolicyVerificationResponse();
                cdpServicePolicyVerificationResponse.setStatusCode(CDPServicePolicyVerificationResponse.NOT_FOUND);
                cdpServicePolicyVerificationResponse.setServiceName(serviceName);
                cdpServicePolicyVerificationResponse.setServiceStatus(e.getMessage());
                cdpServicePolicyVerificationResponses.add(cdpServicePolicyVerificationResponse);
            }
        }
        return new CDPServicePolicyVerificationResponses(cdpServicePolicyVerificationResponses);
    }

    private void verifyCredentialsPermission(AwsCredentialView awsCredential, String policyJson)
            throws AwsPermissionMissingException {
        awsCredentialVerifier.validateAws(awsCredential, policyJson);
    }

    private CloudCredentialStatus determineDefaultRegion(CloudCredential cloudCredential, CloudCredentialStatus credentialStatus) {
        boolean defaultRegionChanged = determineDefaultRegionViaDescribingRegions(cloudCredential);
        if (defaultRegionChanged) {
            credentialStatus = new CloudCredentialStatus(credentialStatus, defaultRegionChanged);
        }
        return credentialStatus;
    }

    private String getErrorMessageForAwsClientException(AwsCredentialView awsCredential, SdkException ae) {
        String errorMessage = String.format("Unable to verify AWS credential due to: '%s'", ae.getMessage());
        if (ae.getMessage().contains("Unable to load AWS credentials")) {
            errorMessage = String.format("Unable to load AWS credentials: please make sure that you configured your assumer %s and %s to deployer.",
                    awsCredential.isGovernmentCloudEnabled() ? "AWS_GOV_ACCESS_KEY_ID" : "AWS_ACCESS_KEY_ID",
                    awsCredential.isGovernmentCloudEnabled() ? "AWS_GOV_SECRET_ACCESS_KEY" : "AWS_SECRET_ACCESS_KEY");
        } else if (ae.getMessage().contains(ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR)) {
            errorMessage = String.format("CDP Control Pane is not authorized to perform sts:AssumeRole on '%s' role", awsCredential.getRoleArn());
        }
        return errorMessage;
    }

    private CloudCredentialStatus verifyAccessKeySecretKeyIsAssumable(CloudCredential cloudCredential) {
        AwsCredentialView awsCredential = new AwsCredentialView(cloudCredential);
        CloudCredentialStatus credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.VERIFIED);
        try {
            boolean defaultRegionChanged = determineDefaultRegionViaDescribingRegions(cloudCredential);
            credentialStatus = verifyCredentialsPermission(cloudCredential, awsCredential, credentialStatus);
            if (defaultRegionChanged) {
                credentialStatus = new CloudCredentialStatus(credentialStatus, defaultRegionChanged);
            }
        } catch (SdkException ae) {
            String errorMessage = "Unable to verify AWS credentials: "
                    + "please make sure the access key and secret key is correct. "
                    + ae.getMessage();
            LOGGER.debug(errorMessage, ae);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, ae, errorMessage);
        } catch (RuntimeException e) {
            String errorMessage = String.format("Could not verify keys '%s': check if the keys exists. %s",
                    awsCredential.getAccessKey(), e.getMessage());
            LOGGER.warn(errorMessage, e);
            credentialStatus = new CloudCredentialStatus(cloudCredential, CredentialStatus.FAILED, e, errorMessage);
        }
        return credentialStatus;
    }

    private void checkRoleIsAssumableWithoutExternalId(CredentialVerificationContext credentialVerificationContext,
        AwsCredentialView awsCredential, String accountId) {
        if (credentialVerificationContext.getCreationVerification() && !entitlementService.internalTenant(accountId)) {
            String roleArn = awsCredential.getRoleArn();
            try {
                credentialClient.retrieveSessionCredentialsWithoutExternalId(awsCredential);
                String message = String.format("CDP Control Pane is able to perform 'sts:AssumeRole' on '%s' role without external id. " +
                        "The role is vulnerable and could be exploited by confused deputy attacks. " +
                        "Please update the role's trust relationship to have a condition on external id " +
                        "or re-create the role with external id configured in the create role wizard.", roleArn);
                LOGGER.warn(message);
                throw new AwsConfusedDeputyException(message);
            } catch (SdkException ae) {
                if (ae.getMessage().contains(ROLE_IS_NOT_ASSUMABLE_ERROR_MESSAGE_INDICATOR)) {
                    String msg = String.format("Consider the specified role as secured. " +
                            "CDP Control Pane is not authorized to perform sts:AssumeRole on '%s' role without external Id.", roleArn);
                    LOGGER.info(msg);
                } else {
                    throw ae;
                }
            }
        }
    }

    private boolean determineDefaultRegionViaDescribingRegions(CloudCredential credential) {
        boolean defaultRegionChanged = false;
        String defaultRegion = defaultRegionSelector.determineDefaultRegion(credential);
        if (StringUtils.isNoneEmpty(defaultRegion)) {
            LOGGER.debug("New default region '{}' has been selected for the credential.", defaultRegion);
            credential.getParameter(AwsCredentialView.AWS, Map.class).put(AwsCredentialView.DEFAULT_REGION_KEY, defaultRegion);
            defaultRegionChanged = true;
        }
        return defaultRegionChanged;
    }
}

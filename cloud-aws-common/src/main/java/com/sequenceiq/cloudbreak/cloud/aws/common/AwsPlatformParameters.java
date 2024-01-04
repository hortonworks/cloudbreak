package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL_MILLIS;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.common.model.AwsDiskType;

@Service
public class AwsPlatformParameters implements PlatformParameters {

    public static final String DEDICATED_INSTANCES = "dedicatedInstances";

    private static final Integer START_LABEL = 97;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("xvd", START_LABEL);

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsPlatformParameters.class);

    private static final String CDP_SUB_RESOURCE_DIR = "/cdp";

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    @Qualifier("AwsTagSpecification")
    private TagSpecification tagSpecification;

    @Inject
    private AwsTagValidator awsTagValidator;

    private final Map<AvailabilityZone, VmType> defaultVmTypes = new HashMap<>();

    private Region defaultRegion;

    private VmType defaultVmType;

    private VmRecommendations vmRecommendations;

    private Map<PolicyType, String> credentialPoliciesJson;

    private Map<PolicyType, String> auditPoliciesJson;

    private Map<PolicyType, String> environmentMinimalPoliciesJson;

    private Map<PolicyType, String> cdpBucketAccessPolicyJson;

    private Map<PolicyType, String> cdpDatalakeAdminS3PolicyJson;

    private Map<PolicyType, String> cdpDynamoDbPolicyJson;

    private Map<PolicyType, String> cdpLogPolicyJson;

    private Map<PolicyType, String> cdpRangerAuditS3PolicyJson;

    private Map<PolicyType, String> cdpRangerRazS3PolicyJson;

    private Map<PolicyType, String> cdpIdbrokerPolicyJson;

    private Map<PolicyType, String> cdpDatalakeBackupPolicyJson;

    private Map<PolicyType, String> cdpDatalakeRestorePolicyJson;

    @PostConstruct
    public void init() {
        credentialPoliciesJson = initCBPolicyJson();
        auditPoliciesJson = initAuditPolicyJson();
        vmRecommendations = initVmRecommendations();
        environmentMinimalPoliciesJson = initEnvironmentMinimalJson();
        cdpLogPolicyJson = initCdpLogPolicyJson();
        cdpDynamoDbPolicyJson = initCdpDynamoDbPolicyJson();
        cdpBucketAccessPolicyJson = initCdpBucketAccessPolicyJson();
        cdpRangerAuditS3PolicyJson = initCdpRangerAuditS3PolicyJson();
        cdpRangerRazS3PolicyJson = initCdpRangerRazS3PolicyJson();
        cdpDatalakeAdminS3PolicyJson = initCdpDatalakeAdminS3PolicyJson();
        cdpIdbrokerPolicyJson = initCdpIdbrokerAssumerPolicyJson();
        cdpDatalakeBackupPolicyJson = initCdpDatalakeBackupPolicyJson();
        cdpDatalakeRestorePolicyJson = initCdpDatalakeRestorePolicyJson();
    }

    @Override
    public TagValidator tagValidator() {
        return awsTagValidator;
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings(), diskDisplayName());
    }

    private Map<DiskType, DisplayName> diskDisplayName() {
        Map<DiskType, DisplayName> map = new HashMap<>();
        for (AwsDiskType awsDiskType : AwsDiskType.values()) {
            map.put(diskType(awsDiskType.value()), displayName(awsDiskType.displayName()));
        }
        return map;
    }

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put(AwsDiskType.Standard.value(), VolumeParameterType.MAGNETIC);
        map.put(AwsDiskType.Gp2.value(), VolumeParameterType.SSD);
        map.put(AwsDiskType.Gp3.value(), VolumeParameterType.SSD);
        map.put(AwsDiskType.Ephemeral.value(), VolumeParameterType.EPHEMERAL);
        map.put(AwsDiskType.St1.value(), VolumeParameterType.ST1);
        return map;
    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        for (AwsDiskType diskType : AwsDiskType.values()) {
            disks.add(diskType(diskType.value()));
        }
        return disks;
    }

    private DiskType defaultDiskType() {
        return diskType(AwsDiskType.Standard.value());
    }

    @Override
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("aws", resource);
    }

    @Override
    public String resourceDefinitionInSubDir(String subDir, String resource) {
        return cloudbreakResourceReaderService.resourceDefinitionInSubDir(subDir, "aws", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation(TTL_MILLIS, false, String.class, Optional.of("^[0-9]*$")));
        additionalStackParameterValidations.add(new StackParamValidation(DEDICATED_INSTANCES, false, Boolean.class, Optional.empty()));
        return additionalStackParameterValidations;
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.singletonList(orchestrator(OrchestratorConstants.SALT)), orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public TagSpecification tagSpecification() {
        return tagSpecification;
    }

    @Override
    public VmRecommendations recommendedVms() {
        return vmRecommendations;
    }

    @Override
    public String platforName() {
        return AwsConstants.AWS_PLATFORM.value();
    }

    @Override
    public boolean isAutoTlsSupported() {
        return true;
    }

    public Map<PolicyType, String> getAuditPoliciesJson() {
        return auditPoliciesJson;
    }

    public Map<PolicyType, String> getCredentialPoliciesJson() {
        return credentialPoliciesJson;
    }

    public Map<PolicyType, String> getEnvironmentMinimalPoliciesJson() {
        return environmentMinimalPoliciesJson;
    }

    public Map<PolicyType, String> getCdpBucketAccessPolicyJson() {
        return cdpBucketAccessPolicyJson;
    }

    public Map<PolicyType, String> getCdpDatalakeAdminS3PolicyJson() {
        return cdpDatalakeAdminS3PolicyJson;
    }

    public Map<PolicyType, String> getCdpDynamoDbPolicyJson() {
        return cdpDynamoDbPolicyJson;
    }

    public Map<PolicyType, String> getCdpLogPolicyJson() {
        return cdpLogPolicyJson;
    }

    public Map<PolicyType, String> getCdpRangerAuditS3PolicyJson() {
        return cdpRangerAuditS3PolicyJson;
    }

    public Map<PolicyType, String> getCdpRangerRazS3PolicyJson() {
        return cdpRangerRazS3PolicyJson;
    }

    public Map<PolicyType, String> getCdpIdbrokerPolicyJson() {
        return cdpIdbrokerPolicyJson;
    }

    public Map<PolicyType, String> getCdpDatalakeBackupPolicyJson() {
        return cdpDatalakeBackupPolicyJson;
    }

    public Map<PolicyType, String> getCdpDatalakeRestorePolicyJson() {
        return cdpDatalakeRestorePolicyJson;
    }

    private VmRecommendations initVmRecommendations() {
        VmRecommendations result = null;
        String vmRecommendation = resourceDefinition("vm-recommendation");
        try {
            result = JsonUtil.readValue(vmRecommendation, VmRecommendations.class);
        } catch (IOException e) {
            LOGGER.error("Cannot initialize Virtual machine recommendations for AWS", e);
        }
        return result;
    }

    private Map<PolicyType, String> initCBPolicyJson() {
        String resourceDefinition = resourceDefinition("cb-policy");
        String govResourceDefinition = resourceDefinition("gov-cb-policy");

        return getPolicyJson(resourceDefinition, Optional.of(govResourceDefinition));
    }

    private Map<PolicyType, String> initAuditPolicyJson() {
        String resourceDefinition = resourceDefinition("audit-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initEnvironmentMinimalJson() {
        String resourceDefinition = resourceDefinition("environment-minimal-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpBucketAccessPolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-bucket-access-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpDatalakeAdminS3PolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-datalake-admin-s3-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpDynamoDbPolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-dynamodb-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpLogPolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-log-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpRangerAuditS3PolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-ranger-audit-s3-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpRangerRazS3PolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-ranger-raz-s3-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpDatalakeBackupPolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "datalake-backup-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpDatalakeRestorePolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "datalake-restore-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> initCdpIdbrokerAssumerPolicyJson() {
        String resourceDefinition = resourceDefinitionInSubDir(CDP_SUB_RESOURCE_DIR, "cdp-idbroker-assume-role-policy");
        return getPolicyJson(resourceDefinition);
    }

    private Map<PolicyType, String> getPolicyJson(String resourceDefinition) {
        return getPolicyJson(resourceDefinition, Optional.empty());
    }

    private Map<PolicyType, String> getPolicyJson(String resourceDefinition, Optional<String> govResourceDefinition) {
        String minified = JsonUtil.minify(resourceDefinition);
        String govMinified = minified;
        if (govResourceDefinition.isPresent()) {
            govMinified = JsonUtil.minify(govResourceDefinition.get());
        }
        if (JsonUtil.INVALID_JSON_CONTENT.equals(minified) || JsonUtil.INVALID_JSON_CONTENT.equals(govMinified)) {
            String message = String.format("Cannot initialize Cloudbreak's policies JSON for AWS: %s and AWS_GOV: %s", minified, govMinified);
            LOGGER.info(message);
            throw new CloudConnectorException(message);
        }
        Map<PolicyType, String> policy = new HashMap<>();
        policy.put(PolicyType.GOV, Base64.encodeBase64String(tranformToGovCloud(govMinified).getBytes()));
        policy.put(PolicyType.PUBLIC, Base64.encodeBase64String(minified.getBytes()));
        return policy;
    }

    private String tranformToGovCloud(String minified) {
        return minified.replaceAll(":aws:", ":aws-us-gov:");
    }

}

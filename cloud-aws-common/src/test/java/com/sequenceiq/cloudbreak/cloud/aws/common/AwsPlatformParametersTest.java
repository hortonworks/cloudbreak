package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL_MILLIS;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters.DEDICATED_INSTANCES;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
class AwsPlatformParametersTest {

    private static final List<String> RESOURCES = List.of("cb-policy", "audit-policy", "environment-minimal-policy");

    private static final List<String> GOV_RESOURCES = List.of("gov-cb-policy", "gov-environment-minimal-policy");

    private static final List<String> RESOURCES_IN_SUB_DIR = List.of("cdp-bucket-access-policy", "cdp-datalake-admin-s3-policy", "cdp-dynamodb-policy",
            "cdp-log-policy", "cdp-ranger-audit-s3-policy", "cdp-ranger-raz-s3-policy", "datalake-backup-policy", "datalake-restore-policy",
            "cdp-idbroker-assume-role-policy");

    private static final List<String> GOV_RESOURCES_IN_SUB_DIR = List.of("cdp-log-policy", "cdp-idbroker-assume-role-policy");

    private static final String RESOURCE_JSON = "{\"definition\":\":aws::cdp:\"}";

    private static final String RESOURCE_JSON_BASE64 = Base64.encodeBase64String(RESOURCE_JSON.getBytes());

    private static final String RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64 = Base64.encodeBase64String("{\"definition\":\":aws-us-gov::cdp-us-gov:\"}".getBytes());

    private static final String GOV_RESOURCE_JSON = "{\"definition\":\"govDefinition\"}";

    private static final String GOV_RESOURCE_JSON_BASE64 = Base64.encodeBase64String(GOV_RESOURCE_JSON.getBytes());

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private TagSpecification tagSpecification;

    @Mock
    private AwsTagValidator awsTagValidator;

    @InjectMocks
    private AwsPlatformParameters underTest;

    private final VmRecommendations vmRecommendations = new VmRecommendations();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        lenient().when(cloudbreakResourceReaderService.resourceDefinition("aws", "vm-recommendation"))
                .thenReturn(JsonUtil.writeValueAsString(vmRecommendations));
        RESOURCES.forEach(r -> lenient().when(cloudbreakResourceReaderService.resourceDefinition("aws", r))
                .thenReturn(RESOURCE_JSON));
        GOV_RESOURCES.forEach(r -> lenient().when(cloudbreakResourceReaderService.resourceDefinition("aws", r))
                .thenReturn(GOV_RESOURCE_JSON));
        RESOURCES_IN_SUB_DIR.forEach(r -> lenient().when(cloudbreakResourceReaderService.resourceDefinitionInSubDir("/cdp", "aws", r))
                .thenReturn(RESOURCE_JSON));
        GOV_RESOURCES_IN_SUB_DIR.forEach(r -> lenient().when(cloudbreakResourceReaderService.resourceDefinitionInSubDir("/cdp/gov", "aws", r))
                .thenReturn(GOV_RESOURCE_JSON));
        underTest.init();
    }

    @Test
    void testTagValidator() {
        assertEquals(awsTagValidator, underTest.tagValidator());
    }

    @Test
    void testScriptParams() {
        ScriptParams result = underTest.scriptParams();

        assertEquals("xvd", result.getDiskPrefix());
        assertEquals(97, result.getStartLabel());
    }

    @Test
    void testDiskTypes() {
        List<DiskType> expectedDiskTypes = Arrays.stream(AwsDiskType.values()).map(dt -> diskType(dt.value())).toList();
        Map<String, VolumeParameterType> expectedDiskMappings = Map.of(
                "standard", VolumeParameterType.MAGNETIC,
                "gp2", VolumeParameterType.SSD,
                "gp3", VolumeParameterType.SSD,
                "ephemeral", VolumeParameterType.EPHEMERAL,
                "st1", VolumeParameterType.ST1
        );
        Map<DiskType, DisplayName> expectedDisplayNames = Arrays.stream(AwsDiskType.values())
                .collect(Collectors.toMap(dt -> diskType(dt.value()), dt -> displayName(dt.displayName())));

        DiskTypes result = underTest.diskTypes();

        assertThat(result.types()).hasSameElementsAs(expectedDiskTypes);
        assertThat(result.defaultType()).isEqualTo(diskType(AwsDiskType.Standard.value()));
        assertThat(result.diskMapping()).containsAllEntriesOf(expectedDiskMappings);
        assertThat(result.displayNames()).containsAllEntriesOf(expectedDisplayNames);
    }

    @Test
    void testResourceDefinition() {
        when(cloudbreakResourceReaderService.resourceDefinition("aws", "resource")).thenReturn("definition");

        assertEquals("definition", underTest.resourceDefinition("resource"));
    }

    @Test
    void testResourceDefinitionInSubDir() {
        when(cloudbreakResourceReaderService.resourceDefinitionInSubDir("subDir", "aws", "resource")).thenReturn("definition");

        assertEquals("definition", underTest.resourceDefinitionInSubDir("subDir", "resource"));
    }

    @Test
    void testAdditionalStackParameters() {
        List<StackParamValidation> expected = List.of(
                new StackParamValidation(TTL_MILLIS, false, String.class, Optional.of("^[0-9]*$")),
                new StackParamValidation(DEDICATED_INSTANCES, false, Boolean.class, Optional.empty())
        );

        List<StackParamValidation> result = underTest.additionalStackParameters();
        assertThat(result).hasSize(2);
        assertThat(underTest.additionalStackParameters()).hasSameElementsAs(expected);
    }

    @Test
    void testOrchestratorParams() {
        PlatformOrchestrator result = underTest.orchestratorParams();

        assertThat(result.types()).hasSameElementsAs(List.of(orchestrator(OrchestratorConstants.SALT)));
        assertThat(result.defaultType()).isEqualTo(orchestrator(OrchestratorConstants.SALT));
    }

    @Test
    void testTagSpecification() {
        assertEquals(tagSpecification, underTest.tagSpecification());
    }

    @Test
    void testRecommendedVms() {
        assertEquals(vmRecommendations, underTest.recommendedVms());
    }

    @Test
    void testPlatformName() {
        assertEquals("AWS", underTest.platforName());
    }

    @Test
    void testIsAutoTlsSupported() {
        assertTrue(underTest.isAutoTlsSupported());
    }

    @Test
    void testSpecialParameters() {
        Map<String, Boolean> expectedSpecialParameters = Map.ofEntries(
                Map.entry(PlatformParametersConsts.CUSTOM_INSTANCETYPE, Boolean.FALSE),
                Map.entry(PlatformParametersConsts.NETWORK_IS_MANDATORY, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.UPSCALING_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.DOWNSCALING_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.STARTSTOP_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.REGIONS_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.VOLUME_ATTACHMENT_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.VERTICAL_SCALING_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.DB_SUBNETS_UPDATE_ENABLED, Boolean.FALSE),
                Map.entry(PlatformParametersConsts.DELETE_VOLUMES_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.DISK_TYPE_CHANGE_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.ADD_VOLUMES_SUPPORTED, Boolean.TRUE),
                Map.entry(PlatformParametersConsts.DELAY_DATABASE_START, Boolean.TRUE)
        );

        SpecialParameters result = underTest.specialParameters();

        assertThat(result.getSpecialParameters()).containsExactlyInAnyOrderEntriesOf(expectedSpecialParameters);
    }

    @Test
    void testGetAuditPoliciesJson() {
        assertThat(underTest.getAuditPoliciesJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCredentialPoliciesJson() {
        assertThat(underTest.getCredentialPoliciesJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, GOV_RESOURCE_JSON_BASE64));
    }

    @Test
    void testGetEnvironmentMinimalPoliciesJson() {
        assertThat(underTest.getEnvironmentMinimalPoliciesJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, GOV_RESOURCE_JSON_BASE64));
    }

    @Test
    void testGetCdpBucketAccessPolicyJson() {
        assertThat(underTest.getCdpBucketAccessPolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpDatalakeAdminS3PolicyJson() {
        assertThat(underTest.getCdpDatalakeAdminS3PolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpDynamoDbPolicyJson() {
        assertThat(underTest.getCdpDynamoDbPolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpLogPolicyJson() {
        assertThat(underTest.getCdpLogPolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, GOV_RESOURCE_JSON_BASE64));
    }

    @Test
    void testGetCdpRangerAuditS3PolicyJson() {
        assertThat(underTest.getCdpRangerAuditS3PolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpRangerRazS3PolicyJson() {
        assertThat(underTest.getCdpRangerRazS3PolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpDatalakeBackupPolicyJson() {
        assertThat(underTest.getCdpDatalakeBackupPolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpDatalakeRestorePolicyJson() {
        assertThat(underTest.getCdpDatalakeRestorePolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, RESOURCE_JSON_TRANSFORMED_TO_GOV_BASE64));
    }

    @Test
    void testGetCdpIdbrokerPolicyJson() {
        assertThat(underTest.getCdpIdbrokerPolicyJson()).containsExactlyInAnyOrderEntriesOf(Map.of(
                PolicyType.PUBLIC, RESOURCE_JSON_BASE64,
                PolicyType.GOV, GOV_RESOURCE_JSON_BASE64));
    }
}

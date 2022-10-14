package com.sequenceiq.cloudbreak.service.consumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu.KuduRoles;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceTest {

    private static final boolean CONSUMPTION_ENABLED = true;

    private static final boolean CONSUMPTION_DISABLED = false;

    private static final boolean HAS_BLUEPRINT_ENABLED = true;

    private static final boolean HAS_BLUEPRINT_DISABLED = false;

    private static final boolean HAS_BLUEPRINT_TEXT_ENABLED = true;

    private static final boolean HAS_BLUEPRINT_TEXT_DISABLED = false;

    private static final String ACCOUNT_ID = "accountId";

    private static final String INITIATOR_USER_CRN = "initiatorUserCrn";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:uuid";

    private static final String STACK_NAME = "datalakeName";

    private static final String BLUEPRINT_TEXT = "blueprintText";

    private static final String HOST_GROUP = "master";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ConsumptionClientService consumptionClientService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private ConsumptionService underTest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Captor
    private ArgumentCaptor<CloudResourceConsumptionRequest> cloudResourceConsumptionRequestCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "consumptionEnabled", CONSUMPTION_ENABLED);
    }

    @Test
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenDeploymentFlagDisabled() {
        ReflectionTestUtils.setField(underTest, "consumptionEnabled", CONSUMPTION_DISABLED);

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto());

        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenEntitlementDisabled() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto());

        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @ParameterizedTest(name = "cloudPlatform={0}")
    @EnumSource(value = CloudPlatform.class, names = { "AWS" }, mode = EnumSource.Mode.EXCLUDE)
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNotSupportedCloudPlatform(CloudPlatform cloudPlatform) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(cloudPlatform, StackType.WORKLOAD, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_ENABLED);

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @ParameterizedTest(name = "stackType={0}")
    @EnumSource(value = StackType.class, names = { "WORKLOAD" }, mode = EnumSource.Mode.EXCLUDE)
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNotDataHub(StackType stackType) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(CloudPlatform.AWS, stackType, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_ENABLED);

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNoBlueprint() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(CloudPlatform.AWS, StackType.WORKLOAD, HAS_BLUEPRINT_DISABLED, HAS_BLUEPRINT_TEXT_ENABLED);

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNoBlueprintText() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(CloudPlatform.AWS, StackType.WORKLOAD, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_DISABLED);

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNoKudu() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        initCmTemplateProcessor(Set.of(), Set.of());

        underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto());

        verify(consumptionClientService, never()).
                scheduleCloudResourceConsumptionCollection(anyString(), any(CloudResourceConsumptionRequest.class), anyString());
    }

    static Object[][] kuduDataProvider() {
        return new Object[][]{
                // hostGroupsWithKuduMaster hostGroupsWithKuduTserver
                {Set.of(HOST_GROUP), Set.of()},
                {Set.of(), Set.of(HOST_GROUP)},
                {Set.of(HOST_GROUP), Set.of(HOST_GROUP)},
        };
    }

    @ParameterizedTest(name = "hostGroupsWithKuduMaster={0}, hostGroupsWithKuduTserver={1}")
    @MethodSource("kuduDataProvider")
    void scheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestExecute(Set<String> hostGroupsWithKuduMaster, Set<String> hostGroupsWithKuduTserver) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        initCmTemplateProcessor(hostGroupsWithKuduMaster, hostGroupsWithKuduTserver);

        ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.scheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto()));

        verify(consumptionClientService).
                scheduleCloudResourceConsumptionCollection(eq(ACCOUNT_ID), cloudResourceConsumptionRequestCaptor.capture(), anyString());
        verifyCloudResourceConsumptionRequest(cloudResourceConsumptionRequestCaptor.getValue());
    }

    @Test
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenDeploymentFlagDisabled() {
        ReflectionTestUtils.setField(underTest, "consumptionEnabled", CONSUMPTION_DISABLED);

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto());

        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenEntitlementDisabled() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto());

        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @ParameterizedTest(name = "cloudPlatform={0}")
    @EnumSource(value = CloudPlatform.class, names = { "AWS" }, mode = EnumSource.Mode.EXCLUDE)
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNotSupportedCloudPlatform(CloudPlatform cloudPlatform) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(cloudPlatform, StackType.WORKLOAD, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_ENABLED);

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @ParameterizedTest(name = "stackType={0}")
    @EnumSource(value = StackType.class, names = { "WORKLOAD" }, mode = EnumSource.Mode.EXCLUDE)
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNotDataHub(StackType stackType) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(CloudPlatform.AWS, stackType, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_ENABLED);

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNoBlueprint() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(CloudPlatform.AWS, StackType.WORKLOAD, HAS_BLUEPRINT_DISABLED, HAS_BLUEPRINT_TEXT_ENABLED);

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNoBlueprintText() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        StackDto stackDto = stackDto(CloudPlatform.AWS, StackType.WORKLOAD, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_DISABLED);

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto);

        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
        verify(cmTemplateProcessorFactory, never()).get(anyString());
    }

    @Test
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestSkipWhenNoKudu() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        initCmTemplateProcessor(Set.of(), Set.of());

        underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto());

        verify(consumptionClientService, never()).unscheduleCloudResourceConsumptionCollection(anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "hostGroupsWithKuduMaster={0}, hostGroupsWithKuduTserver={1}")
    @MethodSource("kuduDataProvider")
    void unscheduleAttachedVolumeConsumptionCollectionForStackIfNeededTestExecute(Set<String> hostGroupsWithKuduMaster, Set<String> hostGroupsWithKuduTserver) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        initCmTemplateProcessor(hostGroupsWithKuduMaster, hostGroupsWithKuduTserver);

        ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.unscheduleAttachedVolumeConsumptionCollectionForStackIfNeeded(stackDto()));

        verify(consumptionClientService).unscheduleCloudResourceConsumptionCollection(ACCOUNT_ID, STACK_CRN, STACK_CRN, INITIATOR_USER_CRN);
    }

    private StackDto stackDto() {
        return stackDto(CloudPlatform.AWS, StackType.WORKLOAD, HAS_BLUEPRINT_ENABLED, HAS_BLUEPRINT_TEXT_ENABLED);
    }

    private StackDto stackDto(CloudPlatform cloudPlatform, StackType stackType, boolean hasBlueprint, boolean hasBlueprintText) {
        StackView stack = stack(cloudPlatform, stackType);
        Blueprint blueprint = hasBlueprint ? blueprint(hasBlueprintText) : null;
        return new StackDto(stack, null, null, null, null, null, null, blueprint, null, null, null, null, null, null, null, null);
    }

    private Blueprint blueprint(boolean hasBlueprintText) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(hasBlueprintText ? BLUEPRINT_TEXT : null);
        return blueprint;
    }

    private Stack stack(CloudPlatform cloudPlatform, StackType stackType) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setResourceCrn(STACK_CRN);
        stack.setName(STACK_NAME);
        stack.setCloudPlatform(cloudPlatform.name());
        stack.setType(stackType);
        return stack;
    }

    private void verifyCloudResourceConsumptionRequest(CloudResourceConsumptionRequest cloudResourceConsumptionRequest) {
        assertThat(cloudResourceConsumptionRequest).isNotNull();
        assertThat(cloudResourceConsumptionRequest.getEnvironmentCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(cloudResourceConsumptionRequest.getMonitoredResourceCrn()).isEqualTo(STACK_CRN);
        assertThat(cloudResourceConsumptionRequest.getMonitoredResourceName()).isEqualTo(STACK_NAME);
        assertThat(cloudResourceConsumptionRequest.getMonitoredResourceType()).isEqualTo(ResourceType.DATAHUB);
        assertThat(cloudResourceConsumptionRequest.getCloudResourceId()).isEqualTo(STACK_CRN);
        assertThat(cloudResourceConsumptionRequest.getConsumptionType()).isEqualTo(ConsumptionType.EBS);
    }

    private void initCmTemplateProcessor(Set<String> hostGroupsWithKuduMaster, Set<String> hostGroupsWithKuduTserver) {
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getHostGroupsWithComponent(KuduRoles.KUDU_MASTER)).thenReturn(hostGroupsWithKuduMaster);
        when(cmTemplateProcessor.getHostGroupsWithComponent(KuduRoles.KUDU_TSERVER)).thenReturn(hostGroupsWithKuduTserver);
    }

}
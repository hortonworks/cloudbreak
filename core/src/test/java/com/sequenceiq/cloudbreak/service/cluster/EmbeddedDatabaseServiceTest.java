package com.sequenceiq.cloudbreak.service.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDatabaseServiceTest {
    private static final String CLOUDPLATFORM = "cloudplatform";

    private static final String ACCOUNT_ID = "6f53f8a0-d5e8-45e6-ab11-cce9b53f7aad";

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:" + UUID.randomUUID();

    private static final Long CLUSTER_ID = 123L;

    private static final String BLUEPRINT_TEXT = "blueprintText";

    private static final String STACK_VERSION_GOOD_MINIMAL = "7.2.2";

    private static final String STACK_VERSION_GOOD = "7.2.15";

    private static final String STACK_VERSION_BAD = "7.2.1";

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private BlueprintService blueprintService;

    @InjectMocks
    private EmbeddedDatabaseService underTest;

    @Mock
    private ClusterView clusterView;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabled() {
        // GIVEN
        StackDto stack = createStack(1);
        when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(true);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenNoDisksAttachedSupported() {
        // GIVEN
        StackDto stack = createStack(0);
        when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(false);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenExternalDBUsed() {
        // GIVEN
        StackDto stack = createStack(0);
        when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.NON_HA);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenExternalDBCrnSet() {
        // GIVEN
        StackDto stack = createStack(0);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn("dbcrn");
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, cluster);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenEmbeddedDbOnRootDisk() {
        // GIVEN
        StackDto stack = createStack(0);
        when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.ON_ROOT_VOLUME);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledByStackView() {
        // GIVEN
        StackView stack = createStackView(1);
        when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(true);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(stack, null);
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledByStackViewWhenNoDisksAttachedSupported() {
        // GIVEN
        StackView stack = createStackView(0);
        when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(false);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledByStackViewWhenExternalDBUsed() {
        // GIVEN
        StackView stack = createStackView(0);
        when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.NON_HA);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledByStackViewWhenExternalDBCrnSet() {
        // GIVEN
        StackView stack = createStackView(0);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn("dbcrn");
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(stack, cluster);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledByStackViewWhenEmbeddedDbOnRootDisk() {
        // GIVEN
        StackView stack = createStackView(0);
        when(stack.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.ON_ROOT_VOLUME);
        // WHEN
        boolean actualResult = underTest.isEmbeddedDatabaseOnAttachedDiskEnabledByStackView(stack, null);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreated() {
        // GIVEN
        StackDto stack = createStack(1);
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreatedWhenNoVolumeAttached() {
        // GIVEN
        StackDto stack = createStack(0);
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreatedWhenNoTemplate() {
        // GIVEN
        StackDto stack = createStackWithoutTemplate();
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(true);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertFalse(actualResult);
    }

    @Test
    public void testIsAttachedDiskForEmbeddedDatabaseCreatedWhenDbOnAttachedDiskIsDisabled() {
        // GIVEN
        StackDto stack = createStack(1);
        Cluster cluster = new Cluster();
        cluster.setEmbeddedDatabaseOnAttachedDisk(false);
        when(stack.getCluster()).thenReturn(cluster);
        // WHEN
        boolean actualResult = underTest.isAttachedDiskForEmbeddedDatabaseCreated(stack);
        // THEN
        assertFalse(actualResult);
    }

    static Object[][] isSslEnforcementForEmbeddedDatabaseEnabledDataProvider() {
        return new Object[][]{
                // stackType, dhEntitlementEnabled, embeddedDatabaseOnAttachedDiskEnabled, resultExpected
                {StackType.DATALAKE, false, false, false},
                {StackType.DATALAKE, false, true, true},
                {StackType.WORKLOAD, false, false, false},
                {StackType.WORKLOAD, false, true, false},
                {StackType.WORKLOAD, true, false, false},
                {StackType.WORKLOAD, true, true, true},
                {StackType.TEMPLATE, true, true, false},
                {StackType.LEGACY, true, true, false},
        };
    }

    @ParameterizedTest()
    @MethodSource("isSslEnforcementForEmbeddedDatabaseEnabledDataProvider")
    void isSslEnforcementForEmbeddedDatabaseEnabledTestWhenPrerequisitesWithGoodRuntime(StackType stackType,
            boolean dhEntitlementEnabled, boolean embeddedDatabaseOnAttachedDiskEnabled, boolean resultExpected) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        Optional<Blueprint> blueprintOptional = Optional.of(blueprint);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(STACK_VERSION_GOOD);

        isSslEnforcementForEmbeddedDatabaseEnabledTestInternal(stackType,
                dhEntitlementEnabled,
                embeddedDatabaseOnAttachedDiskEnabled,
                blueprintOptional,
                resultExpected);
    }

    private void isSslEnforcementForEmbeddedDatabaseEnabledTestInternal(StackType stackType, boolean dhEntitlementEnabled,
            boolean embeddedDatabaseOnAttachedDiskEnabled, Optional<Blueprint> blueprintOptional, boolean resultExpected) {
        StackView stackView;
        if (embeddedDatabaseOnAttachedDiskEnabled) {
            stackView = createStackView(1);
            lenient().when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(true);
        } else {
            stackView = createStackView(0);
            lenient().when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(false);
        }
        when(stackView.getResourceCrn()).thenReturn(STACK_CRN);
        when(stackView.getType()).thenReturn(stackType);

        when(clusterView.getId()).thenReturn(CLUSTER_ID);
        lenient().when(clusterView.getDatabaseServerCrn()).thenReturn(null);

        when(blueprintService.getByClusterId(CLUSTER_ID)).thenReturn(blueprintOptional);

        lenient().when(entitlementService.databaseWireEncryptionDatahubEnabled(ACCOUNT_ID)).thenReturn(dhEntitlementEnabled);

        assertThat(underTest.isSslEnforcementForEmbeddedDatabaseEnabled(stackView, clusterView)).isEqualTo(resultExpected);
    }

    private void isSslEnforcementForEmbeddedDatabaseEnabledTestInternalBlueprintOnly(Optional<Blueprint> blueprintOptional, boolean resultExpected) {
        isSslEnforcementForEmbeddedDatabaseEnabledTestInternal(StackType.DATALAKE, false, true, blueprintOptional, resultExpected);
    }

    @Test
    void isSslEnforcementForEmbeddedDatabaseEnabledTestWhenNoBlueprint() {
        isSslEnforcementForEmbeddedDatabaseEnabledTestInternalBlueprintOnly(Optional.empty(), false);

        verify(cmTemplateProcessorFactory, never()).get(anyString());
        verify(cmTemplateProcessor, never()).getStackVersion();
    }

    @Test
    void isSslEnforcementForEmbeddedDatabaseEnabledTestWhenNoBlueprintText() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(null);

        isSslEnforcementForEmbeddedDatabaseEnabledTestInternalBlueprintOnly(Optional.of(blueprint), false);

        verify(cmTemplateProcessorFactory, never()).get(anyString());
        verify(cmTemplateProcessor, never()).getStackVersion();
    }

    @ParameterizedTest(name = "runtime={0}")
    @ValueSource(strings = {"", " ", STACK_VERSION_BAD})
    @NullSource
    void isSslEnforcementForEmbeddedDatabaseEnabledTestWhenBadRuntime(String runtime) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(runtime);

        isSslEnforcementForEmbeddedDatabaseEnabledTestInternalBlueprintOnly(Optional.of(blueprint), false);
    }

    @ParameterizedTest(name = "runtime={0}")
    @ValueSource(strings = {STACK_VERSION_GOOD_MINIMAL, STACK_VERSION_GOOD})
    void isSslEnforcementForEmbeddedDatabaseEnabledTestWhenGoodRuntime(String runtime) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        when(cmTemplateProcessorFactory.get(BLUEPRINT_TEXT)).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getStackVersion()).thenReturn(runtime);

        isSslEnforcementForEmbeddedDatabaseEnabledTestInternalBlueprintOnly(Optional.of(blueprint), true);
    }

    private StackDto createStack(int volumeCount) {
        StackDto stack = mock(StackDto.class);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        Template template = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        volumeTemplate.setUsageType(VolumeUsageType.DATABASE);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        masterGroup.setTemplate(template);
        lenient().when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(masterGroup, List.of(instanceMetaData))));
        lenient().when(stack.getCloudPlatform()).thenReturn(CLOUDPLATFORM);
        lenient().when(stack.getGatewayGroup()).thenReturn(Optional.of(masterGroup));
        return stack;
    }

    private StackDto createStackWithoutTemplate() {
        StackDto stack = mock(StackDto.class);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        lenient().when(stack.getInstanceGroupDtos()).thenReturn(List.of(new InstanceGroupDto(masterGroup, List.of(instanceMetaData))));
        return stack;
    }

    private StackView createStackView(int volumeCount) {
        StackView stack = mock(StackView.class);
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        Template template = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        volumeTemplate.setUsageType(VolumeUsageType.DATABASE);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        masterGroup.setTemplate(template);
        lenient().when(stack.getCloudPlatform()).thenReturn(CLOUDPLATFORM);
        return stack;
    }
}

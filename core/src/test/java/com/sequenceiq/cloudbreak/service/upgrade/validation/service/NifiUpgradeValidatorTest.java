package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@ExtendWith(MockitoExtension.class)
public class NifiUpgradeValidatorTest {

    private static final String CLUSTER_NAME = "Cluster-1";

    private static final String SERVICE_TYPE = "NIFI";

    private static final String CONFIG = "nifi.working.directory";

    private static final String ROLE_TYPE = "NIFI_NODE";

    private static final String BLUEPRINT_TEXT = "blueprint-text";

    @InjectMocks
    private NifiUpgradeValidator underTest;

    @Mock
    private CmTemplateService cmTemplateService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDto stack;

    private ClusterApi connector;

    @BeforeEach
    public void before() {
        connector = mock(ClusterApi.class);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        Cluster cluster = new Cluster();
        cluster.setExtendedBlueprintText(BLUEPRINT_TEXT);
        cluster.setName(CLUSTER_NAME);
        lenient().when(stack.getBlueprint()).thenReturn(blueprint);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenLockComponentsAndReplaceVmsAreFalseAndTheNifiServiceIsNotPresent() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(false);
        underTest.validate(createRequest(false, false));

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenLockComponentsTrueAndReplaceVmsFalseAndTheNifiServiceIsNotPresent() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(false);

        underTest.validate(createRequest(true, false));

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenLockComponentsFalseAndReplaceVmsTrueAndTheNifiServiceIsNotPresent() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(false);

        underTest.validate(createRequest(false, true));

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheWorkingDirectoryIsCorrect() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of(VolumeUtils.VOLUME_PREFIX));
        Image image = Image.builder()
                .withVersion(CLOUDERA_STACK_VERSION_7_3_1.getVersion())
                .build();
        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(StatedImage.statedImage(image, null, null))
                .build();

        underTest.validate(createRequest(true, false, upgradeImageInfo));
        verify(cmTemplateService).isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    @Test
    public void testValidateShouldThrowExceptionWhenTheWorkingDirectoryIsNotEligibleForUpgrade() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of("/var/etc"));
        Image image = Image.builder()
                .withVersion(CLOUDERA_STACK_VERSION_7_3_1.getVersion())
                .build();
        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(StatedImage.statedImage(image, null, null))
                .build();

        Exception actual = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(createRequest(true, false,
                upgradeImageInfo)));

        assertEquals("Nifi working directory validation failed. The current directory /var/etc is not eligible for upgrade because it is located on the "
                + "root disk. The Nifi working directory should be under the /hadoopfs/fs path. During upgrade or repair the Nifi directory would get deleted "
                + "as the root disk is not kept during these operations.", actual.getMessage());
        verify(cmTemplateService).isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "7.2.17 - Flow Management Light Duty with Apache NiFi, Apache NiFi Registry, Schema Registry",
            "7.2.18 - Flow Management Heavy Duty with Apache NiFi, Apache NiFi Registry, Schema Registry"
    })
    public void testValidateShouldThrowExceptionWhenUpgradingToCdp732WithNifi1Template(String blueprintName) {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        Image image = Image.builder()
                .withVersion(CLOUDERA_STACK_VERSION_7_3_2.getVersion())
                .build();
        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(StatedImage.statedImage(image, null, null))
                .build();
        stack.getBlueprint().setName(blueprintName);

        Exception actual = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(createRequest(true, false,
                upgradeImageInfo)));

        assertEquals("Action Required: Upgrade to NiFi 2.x" + System.lineSeparator()
                        + "The selected CDP Runtime version (7.3.2) does not support NiFi 1.x. A direct, in-place upgrade is not possible. "
                        + "To proceed, you must manually migrate your workflows to a new NiFi 2.x Data Hub cluster before upgrading this environment. "
                        + "Refer to Cloudera Documentation at: https://docs.cloudera.com/dataflow/cloud/migration-tool/topics/cdf-migration-tool.html",
                actual.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "7.2.17 - Flow Management Light Duty with Apache NiFi, Apache NiFi Registry, Schema Registry",
            "7.2.18 - Flow Management Heavy Duty with Apache NiFi, Apache NiFi Registry, Schema Registry"
    })
    public void testValidateShouldThrowExceptionWhenUpgradingToCdp732WithNifi1TemplateWithoutOsUpgrade(String blueprintName) {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        Image image = Image.builder()
                .withVersion(CLOUDERA_STACK_VERSION_7_3_2.getVersion())
                .build();
        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(StatedImage.statedImage(image, null, null))
                .build();
        stack.getBlueprint().setName(blueprintName);

        Exception actual = assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(createRequest(false, false,
                upgradeImageInfo)));

        assertEquals("Action Required: Upgrade to NiFi 2.x" + System.lineSeparator()
                        + "The selected CDP Runtime version (7.3.2) does not support NiFi 1.x. A direct, in-place upgrade is not possible. "
                        + "To proceed, you must manually migrate your workflows to a new NiFi 2.x Data Hub cluster before upgrading this environment. "
                        + "Refer to Cloudera Documentation at: https://docs.cloudera.com/dataflow/cloud/migration-tool/topics/cdf-migration-tool.html",
                actual.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "7.2.17 - Flow Management Light Duty with Apache NiFi, Apache NiFi Registry, Schema Registry",
            "7.2.18 - Flow Management Heavy Duty with Apache NiFi, Apache NiFi Registry, Schema Registry"
    })
    public void testValidateShouldNotThrowExceptionWhenUpgradingToCdp731WithNifi1Template(String blueprintName) {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of(VolumeUtils.VOLUME_PREFIX));
        Image image = Image.builder()
                .withVersion(CLOUDERA_STACK_VERSION_7_3_1.getVersion())
                .build();
        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(StatedImage.statedImage(image, null, null))
                .build();
        stack.getBlueprint().setName(blueprintName);

        assertDoesNotThrow(() -> underTest.validate(createRequest(true, false, upgradeImageInfo)));
        verify(cmTemplateService).isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenUpgradingToCdp731WithNifi2Template() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of(VolumeUtils.VOLUME_PREFIX));
        Image image = Image.builder()
                .withVersion(CLOUDERA_STACK_VERSION_7_3_1.getVersion())
                .build();
        UpgradeImageInfo upgradeImageInfo = UpgradeImageInfo.builder()
                .withTargetStatedImage(StatedImage.statedImage(image, null, null))
                .build();
        stack.getBlueprint().setName("7.2.18 - NiFi 2 - Flow Management Light Duty with Apache NiFi, Apache NiFi Registry, Schema Registry");

        assertDoesNotThrow(() -> underTest.validate(createRequest(true, false,
                upgradeImageInfo)));
        verify(cmTemplateService).isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean lockComponents, boolean replaceVms) {
        return createRequest(lockComponents, replaceVms, null);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean lockComponents, boolean replaceVms, UpgradeImageInfo upgradeImageInfo) {
        return new ServiceUpgradeValidationRequest(stack, lockComponents, true, upgradeImageInfo, replaceVms);
    }
}
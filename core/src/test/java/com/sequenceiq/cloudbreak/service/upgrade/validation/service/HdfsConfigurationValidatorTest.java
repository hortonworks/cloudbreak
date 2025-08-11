package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HDFS_CLIENT_CONFIG_SAFETY_VALVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@ExtendWith(MockitoExtension.class)
class HdfsConfigurationValidatorTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String STACK_NAME = "stack-name";

    private static final String HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY_PROPERTY =
            "<property><name>dfs.client.block.write.replace-datanode-on-failure.policy</name><value>NEVER</value></property>";

    private static final String HDFS_DATANODE_ON_FAILURE_ENABLED_PROPERTY =
            "<property><name>dfs.client.block.write.replace-datanode-on-failure.enable</name><value>false</value></property>";

    private static final String BLUEPRINT_TEXT = "blueprint";

    @InjectMocks
    private HdfsConfigurationValidator underTest;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stack;

    @Mock
    private ClusterApi connector;

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @BeforeEach
    void before() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        lenient().when(stack.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsNotEnabled() {
        underTest.validate(createRequest(false));
        verifyNoInteractions(clusterApiConnectors, connector, entitlementService, clusterTemplateGeneratorService);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsEnabledAndSkipRollingUpgradeValidationIsEnabled() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(true);

        doAs(ACTOR, () -> underTest.validate(createRequest(true)));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(any());
        verifyNoInteractions(clusterApiConnectors, connector, clusterTemplateGeneratorService);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheHdfsServiceIsNotPresentInTheCluster() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(createSupportedServices(Set.of("HIVE", "RANGER")));

        doAs(ACTOR, () -> underTest.validate(createRequest(true)));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(any());
        verifyNoInteractions(clusterApiConnectors, connector);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRequiredConfigsArePresent() {
        String expectedConfig = HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY_PROPERTY + HDFS_DATANODE_ON_FAILURE_ENABLED_PROPERTY;
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(createSupportedServices(Set.of("HIVE", "HDFS", "RANGER")));
        when(connector.getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE))
                .thenReturn(Optional.of(expectedConfig));

        doAs(ACTOR, () -> underTest.validate(createRequest(true)));

        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE);
    }

    @Test
    void testValidateShouldThrowExceptionWhenTheRequiredConfigsAreNotPresent() {
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(createSupportedServices(Set.of("HIVE", "HDFS", "RANGER")));
        when(connector.getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true))));

        assertEquals("Rolling upgrade is not permitted because the value of the dfs.client.block.write.replace-datanode-on-failure.policy configuration "
                + "is incorrect, please set this value to NEVER to enable the rolling upgrade for this cluster and the value of the "
                + "dfs.client.block.write.replace-datanode-on-failure.enable configuration is incorrect, please set this value to FALSE to enable the "
                + "rolling upgrade for this cluster", exception.getMessage());
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE);
    }

    @Test
    void testValidateShouldThrowExceptionWhenOnlyThePolicyTypeIsPresent() {
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(createSupportedServices(Set.of("HIVE", "HDFS", "RANGER")));
        when(connector.getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE))
                .thenReturn(Optional.of(HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY_PROPERTY));

        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true))));

        assertEquals("Rolling upgrade is not permitted because the value of the dfs.client.block.write.replace-datanode-on-failure.enable configuration is "
                + "incorrect, please set this value to FALSE to enable the rolling upgrade for this cluster", exception.getMessage());
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE);
    }

    @Test
    void testValidateShouldThrowExceptionWhenOnlyThePolicyIsPresent() {
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        when(clusterTemplateGeneratorService.getServicesByBlueprint(BLUEPRINT_TEXT)).thenReturn(createSupportedServices(Set.of("HIVE", "HDFS", "RANGER")));
        when(connector.getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE))
                .thenReturn(Optional.of(HDFS_DATANODE_ON_FAILURE_ENABLED_PROPERTY));

        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true))));

        assertEquals("Rolling upgrade is not permitted because the value of the dfs.client.block.write.replace-datanode-on-failure.policy configuration is "
                + "incorrect, please set this value to NEVER to enable the rolling upgrade for this cluster", exception.getMessage());
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(STACK_NAME, HdfsRoles.GATEWAY, HdfsRoles.HDFS, HDFS_CLIENT_CONFIG_SAFETY_VALVE);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean rollingUpgradeEnabled) {
        lenient().when(stack.getName()).thenReturn(STACK_NAME);
        return new ServiceUpgradeValidationRequest(stack, false, rollingUpgradeEnabled, null, false);
    }

    private SupportedServices createSupportedServices(Set<String> services) {
        SupportedServices supportedServices = new SupportedServices();
        supportedServices.setServices(services.stream().map(this::createService).collect(Collectors.toSet()));
        return supportedServices;
    }

    private SupportedService createService(String name) {
        SupportedService supportedService = new SupportedService();
        supportedService.setName(name);
        return supportedService;
    }
}
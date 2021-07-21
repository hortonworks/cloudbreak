package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@RunWith(MockitoJUnitRunner.class)
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

    private ClusterApi connector;

    private Stack stack;

    @Before
    public void before() {
        connector = Mockito.mock(ClusterApi.class);
        stack = createStack();
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenLockComponentsIsFalse() {
        underTest.validate(new ServiceUpgradeValidationRequest(null, false));

        verifyNoInteractions(cmTemplateService);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenLockComponentsTrueAndTheNifiServiceIsNotPresent() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(false);

        underTest.validate(new ServiceUpgradeValidationRequest(stack, true));

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheWorkingDirectoryIsCorrect() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of(VolumeUtils.VOLUME_PREFIX));

        underTest.validate(new ServiceUpgradeValidationRequest(stack, true));
        verify(cmTemplateService).isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    @Test(expected = UpgradeValidationFailedException.class)
    public void testValidateShouldThrowExceptionWhenTheWorkingDirectoryIsNotEligibleForUpgrade() {
        when(cmTemplateService.isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT)).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of("/var/etc"));

        underTest.validate(new ServiceUpgradeValidationRequest(stack, true));
        verify(cmTemplateService).isServiceTypePresent(SERVICE_TYPE, BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    private Stack createStack() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(BLUEPRINT_TEXT);
        Cluster cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        return stack;
    }

}
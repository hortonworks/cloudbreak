package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

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
    public void testValidateShouldNotThrowExceptionWhenLockComponentsIsTrue() {
        underTest.validate(new ServiceUpgradeValidationRequest(null, true));

        verifyNoInteractions(cmTemplateService);
        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenLockComponentsTrueAndTheNifiServiceIsNotPresent() {
        when(cmTemplateService.getServiceTypes(BLUEPRINT_TEXT)).thenReturn(Set.of("SPARK", "ZOOKEEPER"));

        underTest.validate(new ServiceUpgradeValidationRequest(stack, false));

        verifyNoInteractions(clusterApiConnectors);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheWorkingDirectoryIsCorrect() {
        when(cmTemplateService.getServiceTypes(BLUEPRINT_TEXT)).thenReturn(Set.of(SERVICE_TYPE, "ZOOKEEPER"));
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of("/hadoopfs/fs4/etc"));

        underTest.validate(new ServiceUpgradeValidationRequest(stack, false));
        verify(cmTemplateService).getServiceTypes(BLUEPRINT_TEXT);
        verify(clusterApiConnectors).getConnector(stack);
        verify(connector).getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG);
    }

    @Test(expected = UpgradeValidationFailedException.class)
    public void testValidateShouldThrowExceptionWhenTheWorkingDirectoryIsNotEligibleForUpgrade() {
        when(cmTemplateService.getServiceTypes(BLUEPRINT_TEXT)).thenReturn(Set.of(SERVICE_TYPE, "ZOOKEEPER"));
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.getRoleConfigValueByServiceType(CLUSTER_NAME, ROLE_TYPE, SERVICE_TYPE, CONFIG)).thenReturn(Optional.of("/var/etc"));

        underTest.validate(new ServiceUpgradeValidationRequest(stack, false));
        verify(cmTemplateService).getServiceTypes(BLUEPRINT_TEXT);
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
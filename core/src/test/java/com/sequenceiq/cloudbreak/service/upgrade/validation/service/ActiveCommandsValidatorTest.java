package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@ExtendWith(MockitoExtension.class)
public class ActiveCommandsValidatorTest {
    @InjectMocks
    private ActiveCommandsValidator underTest;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi connector;

    @Mock
    private ClusterStatusService clusterStatusService;

    private Stack stack;

    @BeforeEach
    public void before() {
        stack = createStack();
    }

    @Test
    public void testValidateIfActiveCommandsListIsEmpty() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        // WHEN
        underTest.validate(new ServiceUpgradeValidationRequest(stack, true));
        // THEN no exception is thrown
    }

    @Test
    public void testValidateIfActiveCommandsListIsNull() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getActiveCommandsList()).thenReturn(null);
        // WHEN
        underTest.validate(new ServiceUpgradeValidationRequest(stack, true));
        // THEN no exception is thrown
    }

    @Test
    public void testValidateIfActiveCommandsListIsNotEmpty() {
        // GIVEN
        when(clusterApiConnectors.getConnector(stack)).thenReturn(connector);
        when(connector.clusterStatusService()).thenReturn(clusterStatusService);
        when(clusterStatusService.getActiveCommandsList()).thenReturn(List.of("command"));
        // WHEN
        Assertions.assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(new ServiceUpgradeValidationRequest(stack, true)));
        // THEN exception is thrown
    }

    private Stack createStack() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("");
        Cluster cluster = new Cluster();
        cluster.setName("cluster");
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        return stack;
    }
}

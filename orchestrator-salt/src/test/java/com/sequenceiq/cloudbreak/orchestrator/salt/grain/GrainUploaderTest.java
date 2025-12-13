package com.sequenceiq.cloudbreak.orchestrator.salt.grain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker.GrainAddRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltCommandRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@ExtendWith(MockitoExtension.class)
class GrainUploaderTest {

    @Mock
    private SaltCommandRunner saltCommandRunner;

    @Mock
    private ExitCriteria exitCriteria;

    @Mock
    private ExitCriteriaModel exitCriteriaModel;

    @Mock
    private SaltConnector saltConnector;

    @InjectMocks
    private GrainUploader underTest;

    @Test
    void testWhenGrainPropertiesEmpty() throws Exception {
        underTest.uploadGrains(Set.of(), List.of(), exitCriteriaModel, saltConnector, exitCriteria);
        verify(saltCommandRunner, times(0)).runSaltCommand(eq(saltConnector), any(GrainAddRunner.class), eq(exitCriteriaModel),
                eq(exitCriteria));
    }

    @Test
    void testAllGrainsAppendCalled() throws Exception {
        Set<Node> allNodes = Set.of();
        GrainProperties gp1 = new GrainProperties();
        GrainProperties gp2 = new GrainProperties();
        gp1.put("HOST1", Map.of("GRAINKEY1", "GRAINVALUE1", "GRAINKEY2", "GRAINVALUE2"));
        gp1.put("HOST2", Map.of("GRAINKEY1", "GRAINVALUE1", "GRAINKEY2", "GRAINVALUE2"));
        gp1.put("HOST3", Map.of("GRAINKEY3", "GRAINVALUE3", "GRAINKEY2", "GRAINVALUE2"));
        gp1.put("HOST4", Map.of("GRAINKEY5", "GRAINVALUE5"));
        gp1.put("HOST5", Map.of("GRAINKEY6", "GRAINVALUE6", "GRAINKEY7", "GRAINVALUE7"));
        gp2.put("HOST8", Map.of("GRAINKEY8", "GRAINVALUE8", "GRAINKEY9", "GRAINVALUE9"));

        underTest.uploadGrains(allNodes, List.of(gp1, gp2), exitCriteriaModel, saltConnector, exitCriteria);

        ArgumentCaptor<GrainAddRunner> grainAddRunnerArgumentCaptor = ArgumentCaptor.forClass(GrainAddRunner.class);

        verify(saltCommandRunner, atLeastOnce()).runModifyGrainCommand(eq(saltConnector), grainAddRunnerArgumentCaptor.capture(), eq(exitCriteriaModel),
                eq(exitCriteria));
        List<GrainAddRunner> allValues = grainAddRunnerArgumentCaptor.getAllValues();
        assertEquals(8, allValues.size());
        assertGrainHasAllTheHosts(allValues, "GRAINKEY2", "GRAINVALUE2", "HOST1", "HOST2", "HOST3");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY1", "GRAINVALUE1", "HOST1", "HOST2");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY3", "GRAINVALUE3", "HOST3");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY5", "GRAINVALUE5", "HOST4");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY6", "GRAINVALUE6", "HOST5");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY7", "GRAINVALUE7", "HOST5");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY8", "GRAINVALUE8", "HOST8");
        assertGrainHasAllTheHosts(allValues, "GRAINKEY9", "GRAINVALUE9", "HOST8");
    }

    private void assertGrainHasAllTheHosts(List<GrainAddRunner> allValues, String grainKey, String grainValue, String... hosts) {
        assertTrue(allValues.stream().anyMatch(grainAddRunner -> grainAddRunner.getTargetHostnames().containsAll(Set.of(hosts))
                && grainKey.equals(grainAddRunner.getKey()) && grainValue.equals(grainAddRunner.getValue())));
    }

}
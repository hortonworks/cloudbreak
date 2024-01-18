package com.sequenceiq.cloudbreak.conclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.conclusion.step.CmStatusCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.InfoCollectorConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NetworkCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NodeServicesCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.SaltCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.VmStatusCheckerConclusionStep;

class ConclusionStepTreeFactoryTest {

    @Test
    public void testGetConclusionStepTreeWhenDefaultConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.DEFAULT);

        assertNotNull(rootNode);
        assertNull(rootNode.getFailureNode());
        assertEquals(InfoCollectorConclusionStep.class, rootNode.getStepClass());
        ConclusionStepNode saltCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(saltCheckerStepNode);
        assertEquals(SaltCheckerConclusionStep.class, saltCheckerStepNode.getStepClass());

        ConclusionStepNode cmStatusCheckerStepNode = saltCheckerStepNode.getFailureNode();
        assertNotNull(cmStatusCheckerStepNode);
        assertEquals(CmStatusCheckerConclusionStep.class, cmStatusCheckerStepNode.getStepClass());
        ConclusionStepNode vmStatusCheckerStepNode = cmStatusCheckerStepNode.getFailureNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertEquals(VmStatusCheckerConclusionStep.class, vmStatusCheckerStepNode.getStepClass());
        assertNull(vmStatusCheckerStepNode.getFailureNode());
        assertNull(vmStatusCheckerStepNode.getSuccessNode());
        ConclusionStepNode networkCheckerStepNode1 = cmStatusCheckerStepNode.getSuccessNode();
        assertNotNull(networkCheckerStepNode1);
        assertEquals(NetworkCheckerConclusionStep.class, networkCheckerStepNode1.getStepClass());
        assertNull(networkCheckerStepNode1.getFailureNode());
        assertNull(networkCheckerStepNode1.getSuccessNode());

        ConclusionStepNode nodeServicesCheckerStepNode = saltCheckerStepNode.getSuccessNode();
        assertNotNull(nodeServicesCheckerStepNode);
        assertEquals(NodeServicesCheckerConclusionStep.class, nodeServicesCheckerStepNode.getStepClass());
        assertNull(nodeServicesCheckerStepNode.getFailureNode());
        ConclusionStepNode networkCheckerStopNode2 = nodeServicesCheckerStepNode.getSuccessNode();
        assertNotNull(networkCheckerStopNode2);
        assertEquals(NetworkCheckerConclusionStep.class, networkCheckerStopNode2.getStepClass());
        assertNull(networkCheckerStopNode2.getFailureNode());
        assertNull(networkCheckerStopNode2.getSuccessNode());
    }

    @Test
    public void testGetConclusionStepTreeWhenClusterProvisionBeforeSaltBootstrapConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP);

        assertNotNull(rootNode);
        assertEquals(InfoCollectorConclusionStep.class, rootNode.getStepClass());
        assertNull(rootNode.getFailureNode());
        ConclusionStepNode cmStatusCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(cmStatusCheckerStepNode);
        assertEquals(CmStatusCheckerConclusionStep.class, cmStatusCheckerStepNode.getStepClass());
        ConclusionStepNode vmStatusCheckerStepNode = cmStatusCheckerStepNode.getFailureNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertEquals(VmStatusCheckerConclusionStep.class, vmStatusCheckerStepNode.getStepClass());
        assertNull(vmStatusCheckerStepNode.getSuccessNode());
        assertNull(vmStatusCheckerStepNode.getFailureNode());

        assertNull(cmStatusCheckerStepNode.getSuccessNode());
    }

    @Test
    public void testGetConclusionStepTreeWhenClusterProvisionAfterSaltBootstrapConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP);

        assertNotNull(rootNode);
        assertEquals(InfoCollectorConclusionStep.class, rootNode.getStepClass());
        assertNull(rootNode.getFailureNode());
        ConclusionStepNode saltCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(saltCheckerStepNode);
        assertEquals(SaltCheckerConclusionStep.class, saltCheckerStepNode.getStepClass());
        assertNull(saltCheckerStepNode.getSuccessNode());
        ConclusionStepNode cmStatusCheckerStepNode = saltCheckerStepNode.getFailureNode();
        assertNotNull(cmStatusCheckerStepNode);
        assertEquals(CmStatusCheckerConclusionStep.class, cmStatusCheckerStepNode.getStepClass());
        ConclusionStepNode vmStatusCheckerStepNode = cmStatusCheckerStepNode.getFailureNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertEquals(VmStatusCheckerConclusionStep.class, vmStatusCheckerStepNode.getStepClass());
        assertNull(vmStatusCheckerStepNode.getSuccessNode());
        assertNull(vmStatusCheckerStepNode.getFailureNode());

        assertNull(cmStatusCheckerStepNode.getSuccessNode());
    }

    @Test
    public void testGetConclusionStepTreeWhenStackProvisionConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.STACK_PROVISION);

        assertNotNull(rootNode);
        assertEquals(InfoCollectorConclusionStep.class, rootNode.getStepClass());
        assertNull(rootNode.getFailureNode());
        ConclusionStepNode vmStatusCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertEquals(VmStatusCheckerConclusionStep.class, vmStatusCheckerStepNode.getStepClass());
        assertNull(vmStatusCheckerStepNode.getSuccessNode());
        assertNull(vmStatusCheckerStepNode.getFailureNode());
    }
}
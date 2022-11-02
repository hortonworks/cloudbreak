package com.sequenceiq.cloudbreak.conclusion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ConclusionStepTreeFactoryTest {

    @Test
    public void testGetConclusionStepTreeWhenDefaultConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.DEFAULT);

        assertNotNull(rootNode);
        assertNull(rootNode.getFailureNode());
        ConclusionStepNode saltCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(saltCheckerStepNode);

        ConclusionStepNode vmStatusCheckerStepNode = saltCheckerStepNode.getFailureNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertNull(vmStatusCheckerStepNode.getFailureNode());
        ConclusionStepNode networkCheckerStepNode1 = vmStatusCheckerStepNode.getSuccessNode();
        assertNotNull(networkCheckerStepNode1);
        assertNull(networkCheckerStepNode1.getFailureNode());
        assertNull(networkCheckerStepNode1.getSuccessNode());

        ConclusionStepNode nodeServicesCheckerStepNode = saltCheckerStepNode.getSuccessNode();
        assertNotNull(nodeServicesCheckerStepNode);
        assertNull(nodeServicesCheckerStepNode.getFailureNode());
        ConclusionStepNode networkCheckerStopNode2 = nodeServicesCheckerStepNode.getSuccessNode();
        assertNull(networkCheckerStopNode2.getFailureNode());
        assertNull(networkCheckerStopNode2.getSuccessNode());
    }

    @Test
    public void testGetConclusionStepTreeWhenClusterProvisionBeforeSaltBootstrapConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP);

        assertNotNull(rootNode);
        assertNull(rootNode.getFailureNode());
        ConclusionStepNode vmStatusCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertNull(vmStatusCheckerStepNode.getFailureNode());
        assertNull(vmStatusCheckerStepNode.getSuccessNode());
    }

    @Test
    public void testGetConclusionStepTreeWhenClusterProvisionAfterSaltBootstrapConclusionCheckerType() {
        ConclusionStepNode rootNode = ConclusionStepTreeFactory.getConclusionStepTree(ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP);

        assertNotNull(rootNode);
        assertNull(rootNode.getFailureNode());
        ConclusionStepNode saltCheckerStepNode = rootNode.getSuccessNode();
        assertNotNull(saltCheckerStepNode);
        assertNull(saltCheckerStepNode.getSuccessNode());
        ConclusionStepNode vmStatusCheckerStepNode = saltCheckerStepNode.getFailureNode();
        assertNotNull(vmStatusCheckerStepNode);
        assertNull(vmStatusCheckerStepNode.getSuccessNode());
        assertNull(vmStatusCheckerStepNode.getFailureNode());
    }
}
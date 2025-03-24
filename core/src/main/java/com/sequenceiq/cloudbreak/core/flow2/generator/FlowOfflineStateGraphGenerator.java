package com.sequenceiq.cloudbreak.core.flow2.generator;

import com.sequenceiq.flow.graph.OfflineStateGraphGenerator;

public class FlowOfflineStateGraphGenerator {

    private FlowOfflineStateGraphGenerator() {
    }

    public static void main(String[] args) throws Exception {
        String flowConfigsPackageName = "com.sequenceiq.cloudbreak.core.flow2";
        OfflineStateGraphGenerator offlineStateGraphGenerator = new OfflineStateGraphGenerator();
        offlineStateGraphGenerator.collectFlowConfigsAndGenerateGraphs(flowConfigsPackageName);
    }
}

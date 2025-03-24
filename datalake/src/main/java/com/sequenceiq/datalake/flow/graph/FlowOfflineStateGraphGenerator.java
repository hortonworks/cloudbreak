package com.sequenceiq.datalake.flow.graph;

import com.sequenceiq.flow.graph.OfflineStateGraphGenerator;

public class FlowOfflineStateGraphGenerator {

    private FlowOfflineStateGraphGenerator() {
    }

    public static void main(String[] args) throws Exception {
        String flowConfigsPackageName = "com.sequenceiq.datalake.flow";
        OfflineStateGraphGenerator offlineStateGraphGenerator = new OfflineStateGraphGenerator();
        offlineStateGraphGenerator.collectFlowConfigsAndGenerateGraphs(flowConfigsPackageName);
    }
}

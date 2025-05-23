package com.sequenceiq.redbeams.flow.graph;

import com.sequenceiq.flow.graph.OfflineStateGraphGenerator;

public class FlowOfflineStateGraphGenerator {

    private FlowOfflineStateGraphGenerator() {
    }

    public static void main(String[] args) throws Exception {
        String flowConfigsPackageName = "com.sequenceiq.redbeams.flow.redbeams";
        OfflineStateGraphGenerator offlineStateGraphGenerator = new OfflineStateGraphGenerator();
        offlineStateGraphGenerator.collectFlowConfigsAndGenerateGraphs(flowConfigsPackageName);
    }
}

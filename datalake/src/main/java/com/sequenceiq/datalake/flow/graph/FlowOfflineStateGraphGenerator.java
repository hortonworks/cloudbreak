package com.sequenceiq.datalake.flow.graph;

import com.sequenceiq.flow.graph.OfflineStateGraphGenerator;

public class FlowOfflineStateGraphGenerator {

    public static final String FLOW_CONFIGS_PACKAGE_NAME = "com.sequenceiq.datalake.flow";

    private FlowOfflineStateGraphGenerator() {
    }

    public static void main(String[] args) throws Exception {
        OfflineStateGraphGenerator offlineStateGraphGenerator = new OfflineStateGraphGenerator();
        offlineStateGraphGenerator.collectFlowConfigsAndGenerateGraphs(FLOW_CONFIGS_PACKAGE_NAME);
    }
}

package com.sequenceiq.environment.environment.flow.generator;

import com.sequenceiq.flow.graph.OfflineStateGraphGenerator;

public class FlowOfflineStateGraphGenerator {

    public static final String FLOW_CONFIGS_PACKAGE = "com.sequenceiq.environment.environment.flow";

    private FlowOfflineStateGraphGenerator() {
    }

    public static void main(String[] args) throws Exception {
        OfflineStateGraphGenerator offlineStateGraphGenerator = new OfflineStateGraphGenerator();
        offlineStateGraphGenerator.collectFlowConfigsAndGenerateGraphs(FLOW_CONFIGS_PACKAGE);
    }
}

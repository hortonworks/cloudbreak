package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class SetDefaultJavaVersionContext extends CommonContext {

    private final Long stackId;

    private final String defaultJavaVersion;

    public SetDefaultJavaVersionContext(FlowParameters flowParameters, Long stackId, String defaultJavaVersion) {
        super(flowParameters);
        this.stackId = stackId;
        this.defaultJavaVersion = defaultJavaVersion;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }
}

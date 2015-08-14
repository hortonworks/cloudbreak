package com.sequenceiq.cloudbreak.cloud.event.instance;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;

public class BooleanResult {

    private final Boolean result;
    private final CloudContext cloudContext;

    public BooleanResult(CloudContext cloudContext, Boolean result) {
        this.result = result;
        this.cloudContext = cloudContext;
    }

    public Boolean getResult() {
        return result;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InstancesStatusResult{");
        sb.append("result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
package com.sequenceiq.redbeams.flow.redbeams.provision;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RedbeamsContext extends CommonContext {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public RedbeamsContext(FlowParameters flowParameters, CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowParameters);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

}

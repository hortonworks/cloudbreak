package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class InstanceTerminationContext extends CommonContext {

    private final StackView stack;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final List<CloudInstance> cloudInstances;

    private final List<InstanceMetadataView> instanceMetaDataList;

    public InstanceTerminationContext(FlowParameters flowParameters, StackView stack, CloudContext cloudContext, CloudCredential cloudCredential,
        List<CloudInstance> cloudInstances, List<InstanceMetadataView> instanceMetaDataList) {
        super(flowParameters);
        this.stack = stack;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.cloudInstances = cloudInstances;
        this.instanceMetaDataList = instanceMetaDataList;
    }

    public StackView getStack() {
        return stack;
    }

    public Long getStackId() {
        return stack.getId();
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<CloudInstance> getCloudInstances() {
        return cloudInstances;
    }

    public List<InstanceMetadataView> getInstanceMetaDataList() {
        return instanceMetaDataList;
    }
}

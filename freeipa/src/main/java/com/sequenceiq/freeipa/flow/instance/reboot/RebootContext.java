package com.sequenceiq.freeipa.flow.instance.reboot;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

public class RebootContext extends CommonContext {
    private final Stack stack;

    private final List<InstanceMetaData> instanceMetaDataList;

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    public RebootContext(FlowParameters flowParameters, Stack stack, List<InstanceMetaData> instanceMetaDataList,
            CloudContext cloudContext, CloudCredential cloudCredential) {
        super(flowParameters);
        this.stack = stack;
        this.instanceMetaDataList = instanceMetaDataList;
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
    }

    public Stack getStack() {
        return stack;
    }

    public List<InstanceMetaData> getInstanceMetaDataList() {
        return instanceMetaDataList;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getInstanceIds() {
        return getInstanceMetaDataList().stream().map(instanceMetaData -> instanceMetaData.getInstanceId()).collect(Collectors.joining(","));
    }

    public List<String> getInstanceIdList() {
        return getInstanceMetaDataList().stream().map(instanceMetaData -> instanceMetaData.getInstanceId()).collect(Collectors.toList());
    }
}

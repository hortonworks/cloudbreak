package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class CloudInstancesGroupProcessingBatch {

    private final Group group;

    private final long totalInstanceCount;

    private final List<List<CloudInstance>> cloudInstances;

    public CloudInstancesGroupProcessingBatch(Group group, List<List<CloudInstance>> cloudInstances, long totalInstanceCount) {
        this.group = group;
        this.cloudInstances = cloudInstances;
        this.totalInstanceCount = totalInstanceCount;
    }

    public Group getGroup() {
        return group;
    }

    public List<List<CloudInstance>> getCloudInstances() {
        return cloudInstances;
    }

    public long getTotalInstanceCount() {
        return totalInstanceCount;
    }
}

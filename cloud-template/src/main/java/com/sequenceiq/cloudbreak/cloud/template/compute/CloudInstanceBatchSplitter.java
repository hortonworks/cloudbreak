package com.sequenceiq.cloudbreak.cloud.template.compute;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

@Component
public class CloudInstanceBatchSplitter {

    public List<CloudInstancesGroupProcessingBatch> split(Iterable<Group> groups, int batchSize) {

        validate(groups, batchSize);
        List<CloudInstancesGroupProcessingBatch> cloudInstanceProcessingBatches = new ArrayList<>();

        for (Group group : getOrderedCopy(groups)) {
            validate(group);
            List<CloudInstance> instances = group.getInstances().stream()
                    .filter(cloudInstance -> CREATE_REQUESTED.equals(cloudInstance.getTemplate().getStatus()))
                    .toList();

            AtomicInteger counter = new AtomicInteger();
            List<List<CloudInstance>> instancesChunks = instances.stream()
                    .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / batchSize))
                    .values()
                    .stream()
                    .toList();


            CloudInstancesGroupProcessingBatch currentBatch = new CloudInstancesGroupProcessingBatch(group, instancesChunks, instances.size());
            cloudInstanceProcessingBatches.add(currentBatch);

        }

        return cloudInstanceProcessingBatches;
    }

    private void validate(Iterable<Group> groups, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }
        if (groups == null) {
            throw new IllegalArgumentException("Instance groups cannot be null");
        }
    }

    private void validate(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group cannot be null");
        }
        if (group.getInstances() == null) {
            throw new IllegalArgumentException("Instances in group cannot be null");
        }
    }

    private Iterable<Group> getOrderedCopy(Iterable<Group> groups) {
        Ordering<Group> byLengthOrdering = new Ordering<>() {
            @Override
            public int compare(Group left, Group right) {
                return Ints.compare(left.getInstances().size(), right.getInstances().size());
            }
        };
        return byLengthOrdering.sortedCopy(groups);
    }
}

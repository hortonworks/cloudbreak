package com.sequenceiq.cloudbreak.cloud.aws.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AwsGroupView {

    private List<Group> groups;

    private String stackName;

    public AwsGroupView(List<Group> groups, String stackName) {
        this.groups = groups;
    }

    public List<AwsInstanceView> getFlatArmView() {
        List<AwsInstanceView> awsInstances = new ArrayList<>();
        for (Group group : groups) {
            for (InstanceTemplate instance : group.getInstances()) {
                AwsInstanceView novaInstance = new AwsInstanceView(instance, group.getType());
                awsInstances.add(novaInstance);
            }
        }
        return awsInstances;
    }

}
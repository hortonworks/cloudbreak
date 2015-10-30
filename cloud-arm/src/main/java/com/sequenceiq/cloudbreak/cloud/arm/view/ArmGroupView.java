package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class ArmGroupView {

    private List<Group> groups;

    private String stackName;

    public ArmGroupView(List<Group> groups, String stackName) {
        this.groups = groups;
    }

    public List<ArmInstanceView> getFlatArmView() {
        List<ArmInstanceView> armInstances = new ArrayList<>();
        for (Group group : groups) {
            for (CloudInstance instance : group.getInstances()) {
                ArmInstanceView novaInstance = new ArmInstanceView(instance.getTemplate(), group.getType());
                armInstances.add(novaInstance);
            }
        }
        return armInstances;
    }

}
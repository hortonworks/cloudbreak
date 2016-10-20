package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class OpenStackGroupView {

    private final String stackName;

    private final List<Group> groups;

    public OpenStackGroupView(String stackName, List<Group> groups) {
        this.stackName = stackName;
        this.groups = groups;
    }

    public List<NovaInstanceView> getFlatNovaView() {

        List<NovaInstanceView> novaInstances = new ArrayList<>();
        for (Group group : groups) {
            for (CloudInstance instance : group.getInstances()) {
                NovaInstanceView novaInstance = new NovaInstanceView(stackName, instance.getTemplate(), group.getType());
                novaInstances.add(novaInstance);
            }
        }
        return novaInstances;
    }

}

package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;

public class OpenStackGroupView {

    private final String stackName;

    private final List<Group> groups;

    private final Map<String, String> tags;

    public OpenStackGroupView(String stackName, List<Group> groups, Map<String, String> tags) {
        this.stackName = stackName;
        this.groups = groups;
        this.tags = tags;
    }

    public List<NovaInstanceView> getFlatNovaView() {

        List<NovaInstanceView> novaInstances = new ArrayList<>();
        for (Group group : groups) {
            for (CloudInstance instance : group.getInstances()) {
                NovaInstanceView novaInstance = new NovaInstanceView(stackName, instance.getTemplate(), group.getType(), tags);
                novaInstances.add(novaInstance);
            }
        }
        return novaInstances;
    }

}

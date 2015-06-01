package com.sequenceiq.cloudbreak.cloud.openstack.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Instance;

public class OpenStackGroupView {

    private List<Group> groups;

    public OpenStackGroupView(List<Group> groups) {
        this.groups = groups;
    }

    public List<NovaInstanceView> getFlatNovaView() {

        List<NovaInstanceView> novaInstances = new ArrayList<>();
        for (Group group : groups) {
            int index = 0;
            for (Instance instance : group.getInstances()) {
                NovaInstanceView novaInstance = new NovaInstanceView(instance, group.getType(), group.getName(), index);
                novaInstances.add(novaInstance);
            }
            index++;
        }
        return novaInstances;
    }

}

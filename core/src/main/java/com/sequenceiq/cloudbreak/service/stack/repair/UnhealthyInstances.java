package com.sequenceiq.cloudbreak.service.stack.repair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnhealthyInstances {

    private Map<String, List<String>> instancesByGroup = new HashMap<>();

    public void addInstance(String instanceId, String groupName) {
        if (!instancesByGroup.containsKey(groupName)) {
            instancesByGroup.put(groupName, new ArrayList<>());
        }
        List<String> instanceIds = instancesByGroup.get(groupName);
        instanceIds.add(instanceId);
    }

    public Iterable<? extends String> getHostGroups() {
        return instancesByGroup.keySet();
    }

    public List<String> getInstancesForGroup(String groupName) {
        return instancesByGroup.get(groupName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UnhealthyInstances that = (UnhealthyInstances) o;

        return instancesByGroup.equals(that.instancesByGroup);

    }

    @Override
    public int hashCode() {
        return instancesByGroup.hashCode();
    }

    @Override
    public String toString() {
        return "UnhealthyInstances{"
                + "instancesByGroup=" + instancesByGroup
                + '}';
    }
}

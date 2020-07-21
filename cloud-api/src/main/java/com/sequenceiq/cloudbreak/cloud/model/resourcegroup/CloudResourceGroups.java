package com.sequenceiq.cloudbreak.cloud.model.resourcegroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

public class CloudResourceGroups {

    private List<CloudResourceGroup> resourceGroups = new ArrayList<>();

    public CloudResourceGroups() {
    }

    public CloudResourceGroups(@NotNull  List<CloudResourceGroup> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public List<CloudResourceGroup> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(@NotNull List<CloudResourceGroup> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudResourceGroups that = (CloudResourceGroups) o;
        return Objects.equals(resourceGroups, that.resourceGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceGroups);
    }
}

package com.sequenceiq.cloudbreak.cloud.model.resourcegroup;

import java.util.Objects;

public class CloudResourceGroup {

    private String name;

    public CloudResourceGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudResourceGroup that = (CloudResourceGroup) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "CloudResourceGroup{" +
                "name='" + name + '\'' +
                '}';
    }
}

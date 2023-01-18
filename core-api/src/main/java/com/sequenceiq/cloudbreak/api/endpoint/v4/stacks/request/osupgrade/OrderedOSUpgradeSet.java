package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderedOSUpgradeSet {

    private int order;

    private Set<String> instanceIds;

    public OrderedOSUpgradeSet() {
    }

    public OrderedOSUpgradeSet(int order, Set<String> instanceIds) {
        this.order = order;
        this.instanceIds = instanceIds;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public void setInstanceIds(Set<String> instanceIds) {
        this.instanceIds = instanceIds;
    }

    @Override
    public String toString() {
        return "NumberedOsUpgradeSet{" +
                "order=" + order +
                ", instanceIds=" + instanceIds +
                '}';
    }

}

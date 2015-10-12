package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.Resource;

// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public abstract class DeleteContextObject {

    private Long stackId;
    private List<Resource> decommissionResources = new ArrayList<>();

    protected DeleteContextObject(Long stackId) {
        this.stackId = stackId;
    }

    protected DeleteContextObject(Long stackId, List<Resource> decommissionResources) {
        this.stackId = stackId;
        this.decommissionResources = decommissionResources;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public List<Resource> getDecommissionResources() {
        return decommissionResources;
    }

    public void setDecommissionResources(List<Resource> decommissionResources) {
        this.decommissionResources = decommissionResources;
    }
}

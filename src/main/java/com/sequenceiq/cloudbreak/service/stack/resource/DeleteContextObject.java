package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.Resource;

public abstract class DeleteContextObject {

    private Long stackId;
    private List<Resource> decommisionResources = new ArrayList<>();

    protected DeleteContextObject(Long stackId) {
        this.stackId = stackId;
    }

    protected DeleteContextObject(Long stackId, List<Resource> decommisionResources) {
        this.stackId = stackId;
        this.decommisionResources = decommisionResources;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public List<Resource> getDecommisionResources() {
        return decommisionResources;
    }

    public void setDecommisionResources(List<Resource> decommisionResources) {
        this.decommisionResources = decommisionResources;
    }
}

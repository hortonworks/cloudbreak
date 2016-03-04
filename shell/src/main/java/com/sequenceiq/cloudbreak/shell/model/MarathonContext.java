package com.sequenceiq.cloudbreak.shell.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;

@Component
public class MarathonContext {

    private Long selectedMarathonStackId;
    private String selectedMarathonStackName;
    private Set<String> constraintTemplates = new HashSet<>();
    private Map<String, MarathonHostgroupEntry> hostgroups = new HashMap<>();

    public Long getSelectedMarathonStackId() {
        return selectedMarathonStackId;
    }

    public String getSelectedMarathonStackName() {
        return selectedMarathonStackName;
    }

    public void setSelectedMarathonStackName(String selectedMarathonStackName) {
        this.selectedMarathonStackName = selectedMarathonStackName;
    }

    public boolean isSelectedMarathonStackAvailable() {
        return selectedMarathonStackId != null;
    }

    public void resetSelectedMarathonStackId() {
        selectedMarathonStackId = null;
    }

    public void setSelectedMarathonStackId(Long selectedMarathonStackId) {
        this.selectedMarathonStackId = selectedMarathonStackId;
    }

    public Map<String, MarathonHostgroupEntry> getHostgroups() {
        return hostgroups;
    }

    public void resetHostGroups() {
        this.hostgroups = new HashMap<>();
    }

    public void setHostgroups(Map<String, MarathonHostgroupEntry> hostgroups) {
        this.hostgroups = hostgroups;
    }

    public Set<String> getConstraints() {
        return constraintTemplates;
    }

    public void setConstraints(Set<ConstraintTemplateResponse> constraintTemplateResponses) {
        constraintTemplates = new HashSet<>();
        for (ConstraintTemplateResponse constraintTemplateResponse : constraintTemplateResponses) {
            constraintTemplates.add(constraintTemplateResponse.getName());
        }
    }

    public Map<String, MarathonHostgroupEntry> putHostGroup(String name, MarathonHostgroupEntry hostgroupEntry) {
        this.hostgroups.put(name, hostgroupEntry);
        return this.hostgroups;
    }

    public Map<String, MarathonHostgroupEntry> getHostGroups() {
        return hostgroups;
    }

}

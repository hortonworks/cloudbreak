package com.sequenceiq.cloudbreak.domain.view;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonStringSetUtils;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "Environment")
public class EnvironmentView extends CompactView {

    @Column(nullable = false)
    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json regions;

    public Json getRegions() {
        return regions;
    }

    public void setRegions(Json regions) {
        this.regions = regions;
    }

    public Set<String> getRegionsSet() {
        return JsonStringSetUtils.jsonToStringSet(regions);
    }

    public void setRegionsSet(Set<String> regions) {
        this.regions = JsonStringSetUtils.stringSetToJson(regions);
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.ENVIRONMENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnvironmentView that = (EnvironmentView) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}

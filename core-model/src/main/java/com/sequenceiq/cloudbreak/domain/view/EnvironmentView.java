package com.sequenceiq.cloudbreak.domain.view;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;

@Entity
@Table(name = "Environment")
public class EnvironmentView extends CompactView {
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

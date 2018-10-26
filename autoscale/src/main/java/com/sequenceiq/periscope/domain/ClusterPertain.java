package com.sequenceiq.periscope.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class ClusterPertain {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "clusterpertain_generator")
    @SequenceGenerator(name = "clusterpertain_generator", sequenceName = "clusterpertain_id_seq", allocationSize = 1)
    private Long id;

    private String tenant;

    private Long workspaceId;

    private String userId;

    public ClusterPertain() {
    }

    public ClusterPertain(String tenant, Long workspaceId, String userId) {
        this.tenant = tenant;
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

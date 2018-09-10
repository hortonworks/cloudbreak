package com.sequenceiq.cloudbreak.domain.workspace;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonStringSetUtils;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "user_workspace_permissions")
public class UserWorkspacePermissions implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_workspace_permissions")
    @SequenceGenerator(name = "user_workspace_permissions", sequenceName = "user_workspace_permissions_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json permissions;

    @ManyToOne
    private User user;

    @ManyToOne
    private Workspace workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Json getPermissions() {
        return permissions;
    }

    public void setPermissions(Json permissions) {
        this.permissions = permissions;
    }

    public Set<String> getPermissionSet() {
        return JsonStringSetUtils.jsonToStringSet(permissions);
    }

    public void setPermissionSet(Set<String> permissions) {
        this.permissions = JsonStringSetUtils.stringSetToJson(permissions);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
}

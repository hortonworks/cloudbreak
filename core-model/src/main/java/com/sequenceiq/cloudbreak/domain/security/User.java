package com.sequenceiq.cloudbreak.domain.security;

import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonStringSetUtils;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "users")
public class User implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "users_generator")
    @SequenceGenerator(name = "users_generator", sequenceName = "users_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "username")
    private String userName;

    @Basic(optional = false)
    @Column(name = "userid")
    private String userId;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", name = "cloudbreak_permissions")
    private Json cloudbreakPermissions;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", name = "tenant_permissions", nullable = false)
    private Json tenantPermissions;

    @ManyToOne
    private Tenant tenant;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserOrgPermissions> userOrgPermissions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Json getCloudbreakPermissions() {
        return cloudbreakPermissions;
    }

    public void setCloudbreakPermissions(Json cloudbreakPermissions) {
        this.cloudbreakPermissions = cloudbreakPermissions;
    }

    public Set<String> getCloudbreakPermissionSet() {
        return JsonStringSetUtils.jsonToStringSet(cloudbreakPermissions);
    }

    public void setCloudbreakPermissionSet(Set<String> cloudbreakPermissions) {
        this.cloudbreakPermissions = JsonStringSetUtils.stringSetToJson(cloudbreakPermissions);
    }

    public Json getTenantPermissions() {
        return tenantPermissions;
    }

    public void setTenantPermissions(Json tenantPermissions) {
        this.tenantPermissions = tenantPermissions;
    }

    public Set<String> getTenantPermissionSet() {
        return JsonStringSetUtils.jsonToStringSet(tenantPermissions);
    }

    public void setTenantPermissionSet(Set<String> tenantPermissions) {
        this.tenantPermissions = JsonStringSetUtils.stringSetToJson(tenantPermissions);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Set<UserOrgPermissions> getUserOrgPermissions() {
        return userOrgPermissions;
    }

    public void setUserOrgPermissions(Set<UserOrgPermissions> userOrgPermissions) {
        this.userOrgPermissions = userOrgPermissions;
    }
}

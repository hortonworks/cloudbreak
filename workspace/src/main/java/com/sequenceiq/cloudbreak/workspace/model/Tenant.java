package com.sequenceiq.cloudbreak.workspace.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.util.DatabaseUtil;

@Entity
public class Tenant implements Serializable {

    public static final String DEFAULT_NAME = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "tenant_generator")
    @SequenceGenerator(name = "tenant_generator", sequenceName = "tenant_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Workspace> workspaces = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<Workspace> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(Set<Workspace> workspaces) {
        this.workspaces = workspaces;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        Tenant tenant = (Tenant) o;
        return Objects.equals(id, tenant.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", users=" + DatabaseUtil.lazyLoadSafeToString(users, this::usersToString) +
                ", workspaces=" + DatabaseUtil.lazyLoadSafeToString(workspaces, this::workspacesToString) +
                '}';
    }

    private String usersToString() {
        return users.stream().map(User::getUserCrn).collect(Collectors.toSet()).toString();
    }

    private String workspacesToString() {
        return workspaces.stream().map(Workspace::getResourceCrn).collect(Collectors.toSet()).toString();
    }
}

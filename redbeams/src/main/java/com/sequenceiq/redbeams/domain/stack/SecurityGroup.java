package com.sequenceiq.redbeams.domain.stack;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

// import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

@Entity
@Table
public class SecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securitygroup_generator")
    @SequenceGenerator(name = "securitygroup_generator", sequenceName = "securitygroup_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    // @Enumerated(EnumType.STRING)
    // private ResourceStatus status;

    // @OneToMany(mappedBy = "securityGroup", cascade = {CascadeType.REMOVE, CascadeType.PERSIST}, orphanRemoval = true, fetch = FetchType.EAGER)
    // private Set<SecurityRule> securityRules = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "securitygroupid_value")
    private Set<String> securityGroupIds = new HashSet<>();

    // private String cloudPlatform;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // public ResourceStatus getStatus() {
    //     return status;
    // }

    // public void setStatus(ResourceStatus status) {
    //     this.status = status;
    // }

    // public Set<SecurityRule> getSecurityRules() {
    //     return securityRules;
    // }

    // public void setSecurityRules(Set<SecurityRule> securityRules) {
    //     this.securityRules = securityRules;
    // }

    // public String getFirstSecurityGroupId() {
    //     return securityGroupIds == null || securityGroupIds.isEmpty() ? null : securityGroupIds.iterator().next();
    // }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    // public String getCloudPlatform() {
    //     return cloudPlatform;
    // }

    // public void setCloudPlatform(String cloudPlatform) {
    //     this.cloudPlatform = cloudPlatform;
    // }
}

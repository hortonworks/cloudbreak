package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupDetails implements Serializable {
    private Long id;

    private String name;

    private String description;

    private String securityGroupId;

    private Set<SecurityRuleDetails> securityRules;

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

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public Set<SecurityRuleDetails> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(Set<SecurityRuleDetails> securityRules) {
        this.securityRules = securityRules;
    }
}

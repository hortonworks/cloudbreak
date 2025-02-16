package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityGroupDetails implements Serializable {
    private Long id;

    private String name;

    private String description;

    private String securityGroupId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> securityGroupIds = new HashSet<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SecurityRuleDetails> securityRules = new ArrayList<>();

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

    public List<SecurityRuleDetails> getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(List<SecurityRuleDetails> securityRules) {
        this.securityRules = securityRules;
    }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }
}

package com.sequenceiq.environment.network.dao.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Entity
@Where(clause = "archived = false")
@Table(name = "environment_network")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "network_platform")
public abstract class BaseNetwork implements EnvironmentAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_network_generator")
    @SequenceGenerator(name = "environment_network_generator", sequenceName = "environment_network_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne
    @JoinColumn(nullable = false)
    private EnvironmentView environment;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    private String networkCidr;

    @Enumerated(EnumType.STRING)
    private RegistrationType registrationType;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json subnetMetas;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json subnetIds;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    public BaseNetwork() {
        subnetIds = new Json(new HashSet<String>());
        subnetMetas = new Json(new HashMap<String, CloudSubnet>());
    }

    @Override
    public Set<EnvironmentView> getEnvironments() {
        Set<EnvironmentView> environmentViews = new HashSet<>();
        environmentViews.add(environment);
        return environmentViews;
    }

    @Override
    public void setEnvironments(Set<EnvironmentView> environments) {
        if (environments.size() != 1) {
            throw new IllegalArgumentException("Environment set size cannot differ from 1.");
        }
        this.environment = environments.iterator().next();
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public RegistrationType getRegistrationType() {
        return registrationType;
    }

    public Json getSubnetMetas() {
        return subnetMetas;
    }

    public void setSubnetMetas(Map<String, CloudSubnet> subnetMetas) {
        this.subnetMetas = new Json(subnetMetas);
    }

    public Map<String, CloudSubnet> getSubnetMetasMap() {
        return JsonUtil.jsonToType(subnetMetas.getValue(), new TypeReference<>() {
        });
    }

    public Json getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = new Json(subnetIds);
    }

    public Set<String> getSubnetIdsSet() {
        return JsonUtil.jsonToType(subnetIds.getValue(), new TypeReference<>() {
        });
    }

    public void setRegistrationType(RegistrationType registrationType) {
        this.registrationType = registrationType;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }
}

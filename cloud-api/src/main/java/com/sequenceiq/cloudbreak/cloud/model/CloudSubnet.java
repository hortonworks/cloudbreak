package com.sequenceiq.cloudbreak.cloud.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.DeploymentRestriction;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudSubnet extends DynamicModel implements Serializable {

    private String id;

    private String name;

    private String availabilityZone;

    private String cidr;

    private List<String> secondaryCidrs = List.of();

    private Map<String, String> secondaryCidrsWithNames = Map.of();

    private SubnetType type;

    private boolean privateSubnet;

    private boolean mapPublicIpOnLaunch;

    private boolean igwAvailable;

    private Set<DeploymentRestriction> deploymentRestrictions = Set.of();

    public CloudSubnet() {
    }

    public CloudSubnet(CloudSubnet.Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.availabilityZone = builder.availabilityZone;
        this.cidr = builder.cidr;
        this.secondaryCidrs = builder.secondaryCidrs;
        this.type = builder.type;
        this.privateSubnet = builder.privateSubnet;
        this.mapPublicIpOnLaunch = builder.mapPublicIpOnLaunch;
        this.igwAvailable = builder.igwAvailable;
        this.deploymentRestrictions = builder.deploymentRestrictions;
        this.secondaryCidrsWithNames = builder.secondaryCidrsWithNames;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public boolean isPrivateSubnet() {
        return privateSubnet;
    }

    public void setPrivateSubnet(boolean privateSubnet) {
        this.privateSubnet = privateSubnet;
    }

    public boolean isMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public void setMapPublicIpOnLaunch(boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    }

    public boolean isIgwAvailable() {
        return igwAvailable;
    }

    public void setIgwAvailable(boolean igwAvailable) {
        this.igwAvailable = igwAvailable;
    }

    public SubnetType getType() {
        return type;
    }

    public void setType(SubnetType type) {
        this.type = type;
    }

    public Set<DeploymentRestriction> getDeploymentRestrictions() {
        return deploymentRestrictions;
    }

    public void setDeploymentRestrictions(Set<DeploymentRestriction> deploymentRestrictions) {
        this.deploymentRestrictions = deploymentRestrictions;
    }

    public Map<String, String> getSecondaryCidrsWithNames() {
        return secondaryCidrsWithNames;
    }

    public void setSecondaryCidrsWithNames(Map<String, String> secondaryCidrsWithNames) {
        this.secondaryCidrsWithNames = secondaryCidrsWithNames;
    }

    public List<String> getSecondaryCidrs() {
        return secondaryCidrs;
    }

    public void setSecondaryCidrs(List<String> secondaryCidrs) {
        this.secondaryCidrs = secondaryCidrs;
    }

    public CloudSubnet withId(String newId) {
        return new CloudSubnet.Builder()
                .id(newId)
                .name(name)
                .availabilityZone(availabilityZone)
                .cidr(cidr)
                .secondaryCidrs(secondaryCidrs)
                .secondaryCidrsWithNames(secondaryCidrsWithNames)
                .privateSubnet(privateSubnet)
                .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                .igwAvailable(igwAvailable)
                .deploymentRestrictions(deploymentRestrictions)
                .type(type)
                .build();
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CloudSubnet that = (CloudSubnet) o;
        return privateSubnet == that.privateSubnet &&
                mapPublicIpOnLaunch == that.mapPublicIpOnLaunch &&
                igwAvailable == that.igwAvailable &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(availabilityZone, that.availabilityZone) &&
                Objects.equals(cidr, that.cidr) &&
                Objects.equals(secondaryCidrs, that.secondaryCidrs) &&
                Objects.equals(secondaryCidrsWithNames, that.secondaryCidrsWithNames) &&
                Objects.equals(type, that.type) &&
                Objects.equals(deploymentRestrictions, that.deploymentRestrictions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, availabilityZone, cidr, secondaryCidrs, secondaryCidrsWithNames,
                privateSubnet, mapPublicIpOnLaunch, igwAvailable, type, deploymentRestrictions);
    }

    @Override
    public String toString() {
        return "CloudSubnet{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", availabilityZone='" + availabilityZone + '\''
                + ", cidr='" + cidr + '\''
                + ", secondaryCidrs='" + secondaryCidrs + '\''
                + ", secondaryCidrsWithNames='" + secondaryCidrsWithNames + '\''
                + ", privateSubnet=" + privateSubnet
                + ", mapPublicIpOnLaunch=" + mapPublicIpOnLaunch
                + ", igwAvailable=" + igwAvailable
                + ", type=" + type
                + ", deploymentRestrictions=" + deploymentRestrictions
                + ", parameters=" + getParameters()
                + '}';
    }

    public static class Builder {
        private String id;

        private String name;

        private String availabilityZone;

        private String cidr;

        private List<String> secondaryCidrs = List.of();

        private Map<String, String> secondaryCidrsWithNames = Map.of();

        private SubnetType type;

        private boolean privateSubnet;

        private boolean mapPublicIpOnLaunch;

        private boolean igwAvailable;

        private Set<DeploymentRestriction> deploymentRestrictions = Set.of();

        public Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder availabilityZone(String availabilityZone) {
            this.availabilityZone = availabilityZone;
            return this;
        }

        public Builder cidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        public Builder secondaryCidrs(List<String> secondaryCidrs) {
            this.secondaryCidrs = secondaryCidrs;
            return this;
        }

        public Builder secondaryCidrsWithNames(Map<String, String> secondaryCidrsWithNames) {
            this.secondaryCidrsWithNames = secondaryCidrsWithNames;
            return this;
        }

        public Builder type(SubnetType type) {
            this.type = type;
            return this;
        }

        public Builder privateSubnet(boolean privateSubnet) {
            this.privateSubnet = privateSubnet;
            return this;
        }

        public Builder mapPublicIpOnLaunch(boolean mapPublicIpOnLaunch) {
            this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
            return this;
        }

        public Builder igwAvailable(boolean igwAvailable) {
            this.igwAvailable = igwAvailable;
            return this;
        }

        public Builder deploymentRestrictions(Set<DeploymentRestriction> deploymentRestrictions) {
            this.deploymentRestrictions = deploymentRestrictions != null ? deploymentRestrictions : Set.of();
            return this;
        }

        public CloudSubnet build() {
            return new CloudSubnet(this);
        }
    }

}

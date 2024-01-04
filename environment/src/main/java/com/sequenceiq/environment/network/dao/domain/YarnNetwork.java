package com.sequenceiq.environment.network.dao.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@DiscriminatorValue("YARN")
public class YarnNetwork extends BaseNetwork {
    private String queue;

    private Integer lifetime;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    @JsonIgnore
    public String getNetworkId() {
        return null;
    }

    /**
     * Needed to prevent overriding the value set in {@link com.sequenceiq.environment.network.v1.converter.YarnEnvironmentNetworkConverter} with empty value
     */
    @Override
    public void setNetworkCidr(String networkCidr) {
        if (StringUtils.isNotBlank(networkCidr)) {
            super.setNetworkCidr(networkCidr);
        }
    }

    /**
     * Needed to prevent overriding the value set in {@link com.sequenceiq.environment.network.v1.converter.YarnEnvironmentNetworkConverter} with empty value
     */
    @Override
    public void setNetworkCidrs(String networkCidrs) {
        if (StringUtils.isNotBlank(networkCidrs)) {
            super.setNetworkCidrs(networkCidrs);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "YarnNetwork{" +
                "queue='" + queue + '\'' +
                ", lifetime=" + lifetime +
                '}';
    }
}

package com.sequenceiq.environment.network.dao.domain;

import java.util.Objects;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
    public String getNetworkId() {
        return null;
    }

    /**
     * Needed to prevent overriding the value set in {@link com.sequenceiq.environment.network.v1.converter.YarnEnvironmentNetworkConverter} with a null value
     */
    @Override
    public void setNetworkCidr(String networkCidr) {
        if (Objects.nonNull(networkCidr)) {
            super.setNetworkCidr(networkCidr);
            super.setNetworkCidrs(networkCidr);
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

package com.sequenceiq.environment.network.dao.domain;

import java.util.Objects;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("YARN")
public class YarnNetwork extends BaseNetwork {
    private String queue;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
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
        }
    }
}

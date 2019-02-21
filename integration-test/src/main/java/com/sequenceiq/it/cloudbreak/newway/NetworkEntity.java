package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.it.cloudbreak.newway.entity.AbstractCloudbreakEntity;

public class NetworkEntity extends AbstractCloudbreakEntity<NetworkV4Request, NetworkV4Response, NetworkEntity> {
    public static final String NETWORK = "NETWORK";

    NetworkEntity(String newId) {
        super(newId);
        setRequest(new NetworkV4Request());
    }

    NetworkEntity() {
        this(NETWORK);
    }

}

package com.sequenceiq.datalake.service.sdx.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;

@Component
public class NetworkV4ResponseToNetworkV4RequestConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkV4ResponseToNetworkV4RequestConverter.class);

    public NetworkV4Request convert(NetworkV4Response networkV4Response) {
        LOGGER.info("Datalake resize will use network details from the original datalake.");

        NetworkV4Request networkRequest = new NetworkV4Request();

        networkRequest.setSubnetCIDR(networkV4Response.getSubnetCIDR());
        networkRequest.setCloudPlatform(networkV4Response.getCloudPlatform());
        if (networkV4Response.getAws() != null) {
            networkRequest.setAws(networkV4Response.getAws());
        }
        if (networkV4Response.getAzure() != null) {
            networkRequest.setAzure(networkV4Response.getAzure());
        }
        if (networkV4Response.getGcp() != null) {
            networkRequest.setGcp(networkV4Response.getGcp());
        }
        if (networkV4Response.getMock() != null) {
            networkRequest.setMock(networkV4Response.getMock());
        }

        return networkRequest;
    }
}

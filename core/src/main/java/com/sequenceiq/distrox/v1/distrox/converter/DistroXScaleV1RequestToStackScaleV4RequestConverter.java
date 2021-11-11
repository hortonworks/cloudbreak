package com.sequenceiq.distrox.v1.distrox.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkScaleV1Request;

@Component
public class DistroXScaleV1RequestToStackScaleV4RequestConverter {

    public StackScaleV4Request convert(DistroXScaleV1Request source) {
        StackScaleV4Request stackScaleV4Request = new StackScaleV4Request();
        stackScaleV4Request.setDesiredCount(source.getDesiredCount());
        stackScaleV4Request.setGroup(source.getGroup());
        stackScaleV4Request.setAdjustmentType(source.getAdjustmentType());
        stackScaleV4Request.setThreshold(source.getThreshold());
        setStackNetworkScaleRequest(source.getNetworkScaleRequest(), stackScaleV4Request);
        return stackScaleV4Request;
    }

    private void setStackNetworkScaleRequest(NetworkScaleV1Request networkScaleV1Request, StackScaleV4Request stackScaleV4Request) {
        if (networkScaleV1Request != null) {
            NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
            networkScaleV4Request.setPreferredSubnetIds(networkScaleV1Request.getPreferredSubnetIds());
            stackScaleV4Request.setStackNetworkScaleV4Request(networkScaleV4Request);
        }
    }

}

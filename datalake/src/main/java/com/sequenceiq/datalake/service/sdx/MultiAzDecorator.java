package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class MultiAzDecorator {

    public void decorateStackRequestWithMultiAz(StackV4Request stackV4Request, DetailedEnvironmentResponse environment) {
        stackV4Request.setVariant("AWS_NATIVE");
        stackV4Request.getInstanceGroups().forEach(ig -> {
            if (ig.getNetwork() == null) {
                ig.setNetwork(new InstanceGroupNetworkV4Request());
                InstanceGroupAwsNetworkV4Parameters networkParameter = ig.getNetwork().createAws();
                networkParameter.setSubnetIds(new ArrayList<>(environment.getNetwork().getSubnetIds()));
            }
        });
    }
}

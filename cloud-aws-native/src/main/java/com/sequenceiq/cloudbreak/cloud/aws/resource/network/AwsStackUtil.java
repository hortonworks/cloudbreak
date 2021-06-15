package com.sequenceiq.cloudbreak.cloud.aws.resource.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.sequenceiq.cloudbreak.cloud.aws.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.Network;

@Component
public class AwsStackUtil {

    public static final String NO_PUBLIC_IP = "noPublicIp";

    private static final Logger LOGGER = getLogger(AwsStackUtil.class);

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    public boolean isExistingNetwork(AmazonEc2Client amazonEc2Client, Network network) {
        return awsMethodExecutor.execute(() -> {
            DescribeVpcsResult result = amazonEc2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(network.getStringParameter(VPC_ID)));
            return !result.getVpcs().isEmpty();
        }, false);
    }

    public String getVpcId(Network network) {
        return network.getStringParameter(VPC_ID);
    }

    public Boolean noPublicIp(Network network) {
        Boolean noPublicIp = network.getParameter(NO_PUBLIC_IP, Boolean.class);
        if (noPublicIp == null) {
            return Boolean.FALSE;
        }
        return noPublicIp;
    }

    public boolean isExistingSubnet(Network network) {
        return true;
    }

    public String getSubnetId(Network network) {
        return network.getStringParameter(SUBNET_ID);
    }
}

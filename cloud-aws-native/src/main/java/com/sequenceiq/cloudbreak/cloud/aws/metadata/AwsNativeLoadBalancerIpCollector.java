package com.sequenceiq.cloudbreak.cloud.aws.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;

@Component
public class AwsNativeLoadBalancerIpCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeLoadBalancerIpCollector.class);

    @Retryable(retryFor = NotFoundException.class, maxAttempts = 3, backoff = @Backoff(delay = 3000))
    public String getLoadBalancerIp(AmazonEc2Client ec2Client, String loadBalancerName) {
        DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest = DescribeNetworkInterfacesRequest
                .builder().filters(Filter.builder().name("description").values("ELB net/" + loadBalancerName + "/*").build())
                .build();
        DescribeNetworkInterfacesResponse describeNetworkInterfacesResponse = ec2Client.describeNetworkInterfaces(describeNetworkInterfacesRequest);
        List<String> ips = Optional.of(describeNetworkInterfacesResponse.networkInterfaces())
                .map(networkInterfaces -> networkInterfaces.stream()
                        .map(NetworkInterface::privateIpAddress)
                        .filter(Objects::nonNull).toList())
                .orElse(Collections.emptyList());
        if (ips.isEmpty()) {
            String errorMessage = String.format("Failed to get the IP address for load balancer: %s", loadBalancerName);
            LOGGER.debug(errorMessage);
            throw new NotFoundException(errorMessage);
        } else {
            return String.join(",", ips);
        }
    }
}

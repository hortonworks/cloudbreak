package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;

import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

@Service
public class AwsInstanceCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceCommonService.class);

    @Inject
    private CommonAwsClient awsClient;

    public Set<String> getAttachedVolumes(CloudCredential cloudCredential, String region, String instanceId) {
        AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
        AmazonEc2Client ec2Client = awsClient.createAccessWithMinimalRetries(credentialView, region);
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);
        Optional<Reservation> reservation = describeInstancesResponse.reservations()
                .stream()
                .findFirst();

        Set<String> result = new HashSet<>();
        if (reservation.isPresent()) {
            Optional<Instance> instance = reservation.get().instances().stream().findFirst();
            if (instance.isPresent()) {
                result = instance.get().blockDeviceMappings()
                        .stream()
                        .map(e -> e.ebs().volumeId())
                        .collect(Collectors.toSet());
            }
        }
        return result;
    }

    public InstanceTypeMetadata collectInstanceTypes(AuthenticatedContext ac, List<String> instanceIds) {
        DescribeInstancesRequest.Builder describeInstancesRequestBuilder = DescribeInstancesRequest.builder();
        if (!CollectionUtils.isEmpty(instanceIds)) {
            describeInstancesRequestBuilder.instanceIds(instanceIds);
        }
        DescribeInstancesResponse result = new AuthenticatedContextView(ac).getAmazonEC2Client()
                .describeInstances(describeInstancesRequestBuilder.build());
        Map<String, String> instanceTypes = result.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .collect(Collectors.toMap(Instance::instanceId, Instance::instanceTypeAsString));
        return new InstanceTypeMetadata(instanceTypes);
    }
}

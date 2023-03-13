package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

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
}

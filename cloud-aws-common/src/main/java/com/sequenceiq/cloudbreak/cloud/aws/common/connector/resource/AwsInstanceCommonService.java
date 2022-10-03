package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsInstanceCommonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceCommonService.class);

    @Inject
    private CommonAwsClient awsClient;

    public Set<String> getAttachedVolumes(CloudCredential cloudCredential, String region, String instanceId) {
        AwsCredentialView credentialView = new AwsCredentialView(cloudCredential);
        AmazonEc2Client ec2Client = awsClient.createAccessWithMinimalRetries(credentialView, region);
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest()
                .withInstanceIds(instanceId);
        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
        Optional<Reservation> reservation = describeInstancesResult.getReservations()
                .stream()
                .findFirst();

        Set<String> result = new HashSet<>();
        if (reservation.isPresent()) {
            Optional<Instance> instance = reservation.get().getInstances().stream().findFirst();
            if (instance.isPresent()) {
                result = instance.get().getBlockDeviceMappings()
                        .stream()
                        .map(e -> e.getEbs().getVolumeId())
                        .collect(Collectors.toSet());
            }
        }
        return result;
    }
}

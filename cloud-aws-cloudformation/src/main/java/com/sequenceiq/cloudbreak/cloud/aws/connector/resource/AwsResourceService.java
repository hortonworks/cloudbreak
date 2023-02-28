package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstaceStorageInfo;

import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceTypesResponse;
import software.amazon.awssdk.services.ec2.model.DiskInfo;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.InstanceStorageInfo;
import software.amazon.awssdk.services.ec2.model.InstanceType;

@Service
public class AwsResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceService.class);

    @Inject
    private AwsCloudFormationClient awsClient;

    public List<AwsInstaceStorageInfo> getInstanceTypeEphemeralInfo(AuthenticatedContext authenticatedContext, List<String> instanceTypes) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView,
                authenticatedContext.getCloudContext().getLocation().getRegion().value());
        LOGGER.debug("Getting instance information on AWS for instance types: {}", instanceTypes);
        DescribeInstanceTypesResponse instanceTypesResult =  amazonEC2Client.describeInstanceTypes(DescribeInstanceTypesRequest.builder()
                .filters(Filter.builder().name("instance-type").values(instanceTypes).build())
                .instanceTypes(getInstanceTypes(instanceTypes)).build());
        List<InstanceStorageInfo> instanceStorageResults = instanceTypesResult.instanceTypes().stream()
                .filter(software.amazon.awssdk.services.ec2.model.InstanceTypeInfo::instanceStorageSupported)
                .map(software.amazon.awssdk.services.ec2.model.InstanceTypeInfo::instanceStorageInfo).collect(Collectors.toList());
        List<AwsInstaceStorageInfo> awsInstanceStorageResults = Lists.newArrayList();
        instanceStorageResults.forEach(storage -> {
            if (storage.hasDisks()) {
                DiskInfo info = storage.disks().get(0);
                AwsInstaceStorageInfo storageInfo = new AwsInstaceStorageInfo(true,
                        info.count(), Math.toIntExact(info.sizeInGB()));
                awsInstanceStorageResults.add(storageInfo);
            }
        });
        LOGGER.debug("Returning instance storage information for instance types: {}, {}", instanceTypes, instanceStorageResults);
        return awsInstanceStorageResults;
    }

    private List<InstanceType> getInstanceTypes(List<String> instanceTypesList) {
        List<InstanceType> results = Lists.newArrayList();
        for (String instanceType: instanceTypesList) {
            results.add(InstanceType.fromValue(instanceType));
        }
        return results;
    }
}

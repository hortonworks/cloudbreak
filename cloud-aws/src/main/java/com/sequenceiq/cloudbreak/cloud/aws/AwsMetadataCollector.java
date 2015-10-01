package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@Service
public class AwsMetadataCollector implements MetadataCollector {

    @Inject
    private AwsClient awsClient;

    @Override
    public List<CloudVmInstanceStatus> collect(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        List<CloudInstance> cloudInstances = new ArrayList<>();

        try {
           /* AmazonEC2Client access = awsClient.createAccess(authenticatedContext.getCloudCredential());
            DescribeInstancesResult describeResult = access.describeInstances(new DescribeInstancesRequest().withInstanceIds(instances));
            for (Reservation reservation : describeResult.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    for (InstanceMetaData metaData : instanceMetaData) {
                        if (metaData.getInstanceId().equals(instance.getInstanceId())) {
                            String publicIp = instance.getPublicIpAddress();
                            metaData.setPublicIp(publicIp);
                            break;
                        }
                    }
                }
            }*/
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return results;
    }

}

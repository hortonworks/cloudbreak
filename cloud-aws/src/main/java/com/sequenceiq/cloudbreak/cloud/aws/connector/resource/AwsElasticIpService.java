package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@Service
public class AwsElasticIpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsElasticIpService.class);

    private static final String CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID";

    public List<String> getEipsForGatewayGroup(Map<String, String> eipAllocationIds, Group gateway) {
        return eipAllocationIds.entrySet().stream().filter(e -> e.getKey().contains(gateway.getName().replace("_", ""))).map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public void associateElasticIpsToInstances(AmazonEC2 amazonEC2Client, List<String> eipAllocationIds, List<String> instanceIds) {
        if (eipAllocationIds.size() == instanceIds.size()) {
            for (int i = 0; i < eipAllocationIds.size(); i++) {
                associateElasticIpToInstance(amazonEC2Client, eipAllocationIds.get(i), instanceIds.get(i));
            }
        } else {
            LOGGER.warn("The number of elastic ips are not equals with the number of instances. EIP association will be skipped!");
        }
    }

    private void associateElasticIpToInstance(AmazonEC2 amazonEC2Client, String eipAllocationId, String instanceId) {
        AssociateAddressRequest associateAddressRequest = new AssociateAddressRequest()
                .withAllocationId(eipAllocationId)
                .withInstanceId(instanceId);
        amazonEC2Client.associateAddress(associateAddressRequest);
    }

    public Map<String, String> getElasticIpAllocationIds(Map<String, String> outputs, String cFStackName) {
        Map<String, String> elasticIpIds = outputs.entrySet().stream().filter(e -> e.getKey().startsWith(CFS_OUTPUT_EIPALLOCATION_ID))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!elasticIpIds.isEmpty()) {
            return elasticIpIds;
        } else {
            String outputKeyNotFound = String.format("Allocation Id of Elastic IP could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    public void releaseReservedIp(AmazonEC2 client, Iterable<CloudResource> resources) {
        CloudResource elasticIpResource = getReservedIp(resources);
        if (elasticIpResource != null && elasticIpResource.getName() != null) {
            Address address;
            try {
                DescribeAddressesResult describeResult = client.describeAddresses(
                        new DescribeAddressesRequest().withAllocationIds(elasticIpResource.getName()));
                address = describeResult.getAddresses().get(0);
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().equals("The allocation ID '" + elasticIpResource.getName() + "' does not exist")) {
                    LOGGER.warn("Elastic IP with allocation ID '{}' not found. Ignoring IP release.", elasticIpResource.getName());
                    return;
                } else {
                    throw e;
                }
            }
            if (address.getAssociationId() != null) {
                client.disassociateAddress(new DisassociateAddressRequest().withAssociationId(elasticIpResource.getName()));
            }
            client.releaseAddress(new ReleaseAddressRequest().withAllocationId(elasticIpResource.getName()));
        }
    }

    public List<String> getFreeIps(Collection<String> eips, AmazonEC2 amazonEC2Client) {
        DescribeAddressesResult addresses = amazonEC2Client.describeAddresses(new DescribeAddressesRequest().withAllocationIds(eips));
        return addresses.getAddresses().stream().filter(address -> address.getInstanceId() == null)
                .map(Address::getAllocationId).collect(Collectors.toList());
    }

    private CloudResource getReservedIp(Iterable<CloudResource> cloudResources) {
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getType().equals(ResourceType.AWS_RESERVED_IP)) {
                return cloudResource;
            }
        }
        return null;
    }

}

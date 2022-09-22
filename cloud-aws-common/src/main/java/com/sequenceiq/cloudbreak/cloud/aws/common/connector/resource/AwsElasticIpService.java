package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Group;

import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;

@Service
public class AwsElasticIpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsElasticIpService.class);

    private static final String CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID";

    public List<String> getEipsForGatewayGroup(Map<String, String> eipAllocationIds, Group gateway) {
        return eipAllocationIds.entrySet().stream().filter(e -> e.getKey().contains(gateway.getName().replace("_", ""))).map(Entry::getValue)
                .collect(Collectors.toList());
    }

    public List<AssociateAddressResponse> associateElasticIpsToInstances(AmazonEc2Client amazonEC2Client, List<String> eipAllocationIds,
            List<String> instanceIds) {
        List<AssociateAddressResponse> ret = new ArrayList<>();
        if (eipAllocationIds.size() == instanceIds.size()) {
            for (int i = 0; i < eipAllocationIds.size(); i++) {
                ret.add(associateElasticIpToInstance(amazonEC2Client, eipAllocationIds.get(i), instanceIds.get(i)));
            }
        } else {
            LOGGER.warn("The number of elastic ips are not equals with the number of instances. EIP association will be skipped!");
        }
        return ret;
    }

    private AssociateAddressResponse associateElasticIpToInstance(AmazonEc2Client amazonEC2Client, String eipAllocationId, String instanceId) {
        LOGGER.debug("{} eip associated to {}", eipAllocationId, instanceId);
        AssociateAddressRequest associateAddressRequest = AssociateAddressRequest.builder()
                .allocationId(eipAllocationId)
                .instanceId(instanceId)
                .build();
        return amazonEC2Client.associateAddress(associateAddressRequest);
    }

    public Map<String, String> getElasticIpAllocationIds(Map<String, String> outputs, String cFStackName) {
        Map<String, String> elasticIpIds = outputs.entrySet().stream().filter(e -> e.getKey().startsWith(CFS_OUTPUT_EIPALLOCATION_ID))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        if (!elasticIpIds.isEmpty()) {
            return elasticIpIds;
        } else {
            String outputKeyNotFound = String.format("Allocation Id of Elastic IP could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    public List<String> getFreeIps(Collection<String> eips, AmazonEc2Client amazonEC2Client) {
        DescribeAddressesResponse addresses = amazonEC2Client.describeAddresses(DescribeAddressesRequest.builder().allocationIds(eips).build());
        return addresses.addresses().stream().filter(address -> address.instanceId() == null)
                .map(Address::allocationId).collect(Collectors.toList());
    }

}

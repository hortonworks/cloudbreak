package com.sequenceiq.it.cloudbreak.recovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;


public class RecoveryUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryUtil.class);

    private RecoveryUtil() {
    }

    public static String getInstanceId(StackResponse stackResponse, String hostGroup) {
        String instanceId = null;
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();

        outerloop:
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (hostGroup.equals(instanceGroup.getGroup())) {
                Set<InstanceMetaDataJson> instanceMetaData = instanceGroup.getMetadata();
                for (InstanceMetaDataJson metaData : instanceMetaData) {
                    instanceId = metaData.getInstanceId();
                    break outerloop;
                }
            }
        }
        Assert.assertNotNull(instanceId);
        return instanceId;
    }

    public static void deleteInstance(Map<String, String> cloudProviderParams, String instanceId) {
        switch (cloudProviderParams.get("cloudProvider")) {
            case "AWS":
                deleteAWSInstance(Regions.fromName(cloudProviderParams.get("region")), instanceId);
                break;
            case "OPENSTACK":
                deleteOpenstackInstance(cloudProviderParams.get("endpoint"), cloudProviderParams.get("userName"), cloudProviderParams.get("password"),
                        cloudProviderParams.get("tenantName"), instanceId);
                break;
            default:
                LOGGER.info("CloudProvider {} is not supported!", cloudProviderParams.get("cloudProvider"));
                break;
        }
    }

    public static void deleteAWSInstance(Regions region, String instanceId) {
        List<String> idList = new ArrayList<>();
        idList.add(instanceId);

        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();

        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest(idList);
        ec2.terminateInstances(terminateInstancesRequest);
        LOGGER.info("Instance was deleted with id:" + instanceId);
    }

    public static void deleteOpenstackInstance(String endpoint, String userName, String password, String tenantName, String instanceId) {
        OSClient os = OSFactory.builderV2()
                .endpoint(endpoint)
                .credentials(userName, password)
                .tenantName(tenantName)
                .authenticate();

        ServerService servers =  os.compute().servers();
        servers.delete(instanceId);
        LOGGER.info("Instance was deleted with id: " + instanceId);
    }

    public static long getCurentTimeStamp() {
        return Instant.now().getEpochSecond() * 1000;
    }
}
package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Instance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;


@Component
public class OpenStackMetadataCollector {


    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataCollector.class);

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackHeatUtils utils;


    public List<CloudVmInstanceStatus> collectVmMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<Instance> vms) {

        CloudResource resource = utils.getHeatResource(resources);

        String stackName = resource.getName();
        String heatStackId = resource.getReference();

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);

        Map<String, InstanceMetaData> metaDataMap = new HashMap<>();

        List<Map<String, Object>> outputs = heatStack.getOutputs();
        for (Map<String, Object> map : outputs) {
            String instanceUUID = (String) map.get("output_value");
            Server server = client.compute().servers().get(instanceUUID);
            Map<String, String> metadata = server.getMetadata();
            String privateInstanceId = utils.getPrivateInstanceId(metadata);
            InstanceMetaData metaData = createInstanceMetaData(server, instanceUUID);
            metaDataMap.put(privateInstanceId, metaData);
        }

        List<CloudVmInstanceStatus> results = new ArrayList<>();
        for (Instance vm : vms) {
            String privateInstanceId = utils.getPrivateInstanceId(vm.getGroupName(), Integer.toString(vm.getPrivateId()));
            InstanceMetaData metaData = metaDataMap.get(privateInstanceId);
            vm.addMetaData(metaData);
            CloudVmInstanceStatus status = new CloudVmInstanceStatus(vm, InstanceStatus.CREATED);
            results.add(status);
        }

        return results;
    }


    private InstanceMetaData createInstanceMetaData(Server server, String instanceId) {

        String privateIp = null;
        String floatingIp = null;

        Map<String, List<? extends Address>> adrMap = server.getAddresses().getAddresses();
        LOGGER.debug("Address map: {} of instance: {}", adrMap, server.getName());
        for (String key : adrMap.keySet()) {
            LOGGER.debug("Network resource key: {} of instance: {}", key, server.getName());
            List<? extends Address> adrList = adrMap.get(key);
            for (Address adr : adrList) {
                LOGGER.debug("Network resource key: {} of instance: {}, address: {}", key, adr);
                switch (adr.getType()) {
                    case "fixed":
                        privateIp = adr.getAddr();
                        LOGGER.info("PrivateIp of instance: {} is {}", server.getName(), server.getName(), privateIp);
                        break;
                    case "floating":
                        floatingIp = adr.getAddr();
                        LOGGER.info("FloatingIp of instance: {} is {}", server.getName(), floatingIp);
                        break;
                    default:
                        LOGGER.error("No such network resource type: {}, instance: {}", adr.getType(), server.getName());
                }
            }
        }

        InstanceMetaData md = new InstanceMetaData(
                instanceId,
                privateIp,
                floatingIp);

        return md;
    }
}

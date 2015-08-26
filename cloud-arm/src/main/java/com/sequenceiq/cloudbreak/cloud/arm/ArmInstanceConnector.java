package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@Service
public class ArmInstanceConnector implements InstanceConnector {

    @Inject
    private ArmClient armClient;

    @Inject
    private ArmMetadataCollector armMetadataCollector;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        // Currently Azure rm not supporting the get fingerprint method
        return "-----END SSH HOST KEY FINGERPRINTS-----";
    }

    @Override
    public List<CloudVmInstanceStatus> collectMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<InstanceTemplate> vms) {
        return armMetadataCollector.collectVmMetadata(authenticatedContext, resources, vms);
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudInstance> vms) {
        AzureRMClient azureRMClient = armClient.createAccess(ac.getCloudCredential());
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                azureRMClient.startVirtualMachine(ac.getCloudContext().getStackName(), vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudInstance> vms) {
        AzureRMClient azureRMClient = armClient.createAccess(ac.getCloudCredential());
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                azureRMClient.stopVirtualMachine(ac.getCloudContext().getStackName(), vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        AzureRMClient azureRMClient = armClient.createAccess(ac.getCloudCredential());

        for (CloudInstance vm : vms) {
            try {
                Map<String, Object> virtualMachine = azureRMClient.getVirtualMachineInstanceView(ac.getCloudContext().getStackName(), vm.getInstanceId());
                List<Map<String, Object>> vmStatuses = (List) virtualMachine.get("statuses");
                for (Map<String, Object> vmStatuse : vmStatuses) {
                    String statusCode = vmStatuse.get("code").toString();
                    if (statusCode.startsWith("PowerState")) {
                        statusCode = statusCode.replace("PowerState/", "");
                        statuses.add(new CloudVmInstanceStatus(vm, ArmInstanceStatus.get(statusCode)));
                        break;
                    }
                }
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
            }
        }
        return statuses;
    }
}

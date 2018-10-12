package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.INSTANCE_NAME;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class AzureInstanceConnector implements InstanceConnector {

    @Inject
    private AzureUtils armTemplateUtils;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Azure ARM doesn't provide access to the VM console output yet.");
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                String resourceGroupName = armTemplateUtils.getResourceGroupName(ac.getCloudContext(), vm);
                AzureClient azureClient = ac.getParameter(AzureClient.class);
                azureClient.startVirtualMachine(resourceGroupName, vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (RuntimeException e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getMessage()));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                String resourceGroupName = armTemplateUtils.getResourceGroupName(ac.getCloudContext(), vm);
                AzureClient azureClient = ac.getParameter(AzureClient.class);
                azureClient.deallocateVirtualMachine(resourceGroupName, vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (RuntimeException e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getMessage()));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            String resourceGroupName = armTemplateUtils.getResourceGroupName(ac.getCloudContext(), vm);
            try {
                AzureClient azureClient = ac.getParameter(AzureClient.class);
                boolean virtualMachineExists = azureClient.isVirtualMachineExists(resourceGroupName, vm.getInstanceId());
                if (virtualMachineExists) {
                    VirtualMachine virtualMachine = azureClient.getVirtualMachine(resourceGroupName, vm.getInstanceId());
                    PowerState virtualMachinePowerState = virtualMachine.powerState();
                    String computerName = virtualMachine.computerName();
                    vm.putParameter(INSTANCE_NAME, computerName);
                    statuses.add(new CloudVmInstanceStatus(vm, AzureInstanceStatus.get(virtualMachinePowerState)));
                } else {
                    statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
                }
            } catch (CloudException e) {
                if (e.body() != null && "ResourceNotFound".equals(e.body().code())) {
                    statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
                } else {
                    statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.UNKNOWN));
                }
            } catch (RuntimeException ignored) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.UNKNOWN));
            }
        }
        return statuses;
    }
}

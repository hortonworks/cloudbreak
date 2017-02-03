package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.microsoft.azure.management.compute.InstanceViewStatus;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
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
        String stackName = armTemplateUtils.getStackName(ac.getCloudContext());
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                AzureClient azureClient = ac.getParameter(AzureClient.class);
                azureClient.startVirtualMachine(stackName, vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getMessage()));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        String stackName = armTemplateUtils.getStackName(ac.getCloudContext());
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                AzureClient azureClient = ac.getParameter(AzureClient.class);
                azureClient.stopVirtualMachine(stackName, vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getMessage()));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        String stackName = armTemplateUtils.getStackName(ac.getCloudContext());

        for (CloudInstance vm : vms) {
            try {
                AzureClient azureClient = ac.getParameter(AzureClient.class);
                VirtualMachineInstanceView virtualMachine = azureClient.getVirtualMachineInstanceView(stackName, vm.getInstanceId());
                List<InstanceViewStatus> vmStatuses = virtualMachine.statuses();

                for (InstanceViewStatus vmStatus : vmStatuses) {
                    String statusCode = vmStatus.code();
                    if (statusCode.startsWith("PowerState")) {
                        statusCode = statusCode.replace("PowerState/", "");
                        statuses.add(new CloudVmInstanceStatus(vm, AzureInstanceStatus.get(statusCode)));
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

package com.sequenceiq.cloudbreak.cloud.arm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import groovyx.net.http.HttpResponseException;

@Service
public class ArmInstanceConnector implements InstanceConnector {

    @Inject
    private ArmClient armClient;

    @Inject
    private ArmMetadataCollector armMetadataCollector;

    @Inject
    private ArmUtils armTemplateUtils;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Azure ARM doesn't provide access to the VM console output yet.");
    }

    @Override
    public MetadataCollector metadata() {
        return armMetadataCollector;
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        AzureRMClient azureRMClient = armClient.createAccess(ac.getCloudCredential());
        String stackName = armTemplateUtils.getStackName(ac.getCloudContext());
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                azureRMClient.startVirtualMachine(stackName, vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (HttpResponseException e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getResponse().getData().toString()));
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getMessage()));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        AzureRMClient azureRMClient = armClient.createAccess(ac.getCloudCredential());
        String stackName = armTemplateUtils.getStackName(ac.getCloudContext());
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();

        for (CloudInstance vm : vms) {
            try {
                azureRMClient.stopVirtualMachine(stackName, vm.getInstanceId());
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
            } catch (HttpResponseException e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getResponse().getData().toString()));
            } catch (Exception e) {
                statuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.getMessage()));
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        AzureRMClient azureRMClient = armClient.createAccess(ac.getCloudCredential());
        String stackName = armTemplateUtils.getStackName(ac.getCloudContext());

        for (CloudInstance vm : vms) {
            try {
                Map<String, Object> virtualMachine = azureRMClient.getVirtualMachineInstanceView(stackName, vm.getInstanceId());
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

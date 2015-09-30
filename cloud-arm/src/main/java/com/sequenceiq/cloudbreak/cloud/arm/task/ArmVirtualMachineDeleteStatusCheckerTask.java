package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import java.util.Map;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import groovyx.net.http.HttpResponseException;

public class ArmVirtualMachineDeleteStatusCheckerTask extends PollBooleanStateTask {

    private VirtualMachineCheckerContext virtualMachineCheckerContext;
    private ArmClient armClient;

    public ArmVirtualMachineDeleteStatusCheckerTask(AuthenticatedContext ac, ArmClient armClient, VirtualMachineCheckerContext virtualMachineCheckerContext) {
        super(ac, false);
        this.armClient = armClient;
        this.virtualMachineCheckerContext = virtualMachineCheckerContext;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.createAccess(virtualMachineCheckerContext.getArmCredentialView());
        try {
            Map virtualMachine = client.getVirtualMachine(virtualMachineCheckerContext.getGroupName(),
                    virtualMachineCheckerContext.getVirtualMachine());
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString());
            } else {
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }
}

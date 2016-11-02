package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import groovyx.net.http.HttpResponseException;

@Component(ArmVirtualMachineDeleteStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ArmVirtualMachineDeleteStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "armVirtualMachineDeleteStatusCheckerTask";

    private VirtualMachineCheckerContext virtualMachineCheckerContext;

    private ArmClient armClient;

    public ArmVirtualMachineDeleteStatusCheckerTask(AuthenticatedContext ac, ArmClient armClient, VirtualMachineCheckerContext virtualMachineCheckerContext) {
        super(ac, false);
        this.armClient = armClient;
        this.virtualMachineCheckerContext = virtualMachineCheckerContext;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.getClient(virtualMachineCheckerContext.getArmCredentialView());
        try {
            client.getVirtualMachine(virtualMachineCheckerContext.getGroupName(),
                    virtualMachineCheckerContext.getVirtualMachine());
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != NOT_FOUND) {
                throw new CloudConnectorException(e.getResponse().getData().toString());
            } else {
                return true;
            }
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }
}

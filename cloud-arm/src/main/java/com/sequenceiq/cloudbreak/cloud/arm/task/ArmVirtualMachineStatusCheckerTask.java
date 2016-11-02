package com.sequenceiq.cloudbreak.cloud.arm.task;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(ArmVirtualMachineStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ArmVirtualMachineStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "armVirtualMachineStatusCheckerTask";

    private VirtualMachineCheckerContext virtualMachineCheckerContext;

    private ArmClient armClient;

    public ArmVirtualMachineStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient, VirtualMachineCheckerContext
            virtualMachineCheckerContext) {
        super(authenticatedContext, true);
        this.armClient = armClient;
        this.virtualMachineCheckerContext = virtualMachineCheckerContext;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.getClient(virtualMachineCheckerContext.getArmCredentialView());
        try {
            Map virtualMachine = client.getVirtualMachine(virtualMachineCheckerContext.getGroupName(), virtualMachineCheckerContext.getVirtualMachine());
            Map properties = (Map) virtualMachine.get("properties");
            String statusCode = properties.get("provisioningState").toString();
            statusCode = statusCode.replace("PowerState/", "");
            if (virtualMachineCheckerContext.getStatus().equals(statusCode)) {
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }
}

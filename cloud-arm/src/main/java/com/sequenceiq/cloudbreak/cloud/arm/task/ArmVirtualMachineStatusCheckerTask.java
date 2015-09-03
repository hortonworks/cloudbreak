package com.sequenceiq.cloudbreak.cloud.arm.task;

import java.util.Map;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public class ArmVirtualMachineStatusCheckerTask implements BooleanStateConnector {

    private VirtualMachineCheckerContext virtualMachineCheckerContext;
    private ArmClient armClient;

    public ArmVirtualMachineStatusCheckerTask(ArmClient armClient, VirtualMachineCheckerContext virtualMachineCheckerContext) {
        this.armClient = armClient;
        this.virtualMachineCheckerContext = virtualMachineCheckerContext;
    }

    @Override
    public Boolean check(AuthenticatedContext authenticatedContext) {
        AzureRMClient client = armClient.createAccess(virtualMachineCheckerContext.getArmCredentialView());
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

package com.sequenceiq.cloudbreak.cloud.arm.poller;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmTemplateUtils.NOT_FOUND;

import java.util.Map;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.poller.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import groovyx.net.http.HttpResponseException;

public class ArmVirtualMachineDeleteStatusCheckerTask implements BooleanStateConnector {

    private VirtualMachineCheckerContext virtualMachineCheckerContext;
    private ArmClient armClient;

    public ArmVirtualMachineDeleteStatusCheckerTask(ArmClient armClient, VirtualMachineCheckerContext virtualMachineCheckerContext) {
        this.armClient = armClient;
        this.virtualMachineCheckerContext = virtualMachineCheckerContext;
    }

    @Override
    public Boolean check(AuthenticatedContext authenticatedContext) {
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

package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmTemplateUtils.NOT_FOUND;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.NetworkInterfaceCheckerContext;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import groovyx.net.http.HttpResponseException;

public class ArmNetworkInterfaceDeleteStatusCheckerTask implements BooleanStateConnector {

    private NetworkInterfaceCheckerContext networkInterfaceCheckerContext;
    private ArmClient armClient;

    public ArmNetworkInterfaceDeleteStatusCheckerTask(ArmClient armClient, NetworkInterfaceCheckerContext networkInterfaceCheckerContext) {
        this.networkInterfaceCheckerContext = networkInterfaceCheckerContext;
        this.armClient = armClient;
    }

    @Override
    public Boolean check(AuthenticatedContext authenticatedContext) {
        AzureRMClient client = armClient.createAccess(networkInterfaceCheckerContext.getArmCredentialView());
        try {
            Object networkInterface = client.getNetworkInterface(networkInterfaceCheckerContext.getGroupName(),
                    networkInterfaceCheckerContext.getNetworkName());
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

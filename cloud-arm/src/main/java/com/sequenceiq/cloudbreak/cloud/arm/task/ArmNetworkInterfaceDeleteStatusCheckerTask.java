package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.NetworkInterfaceCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import groovyx.net.http.HttpResponseException;

@Component(ArmNetworkInterfaceDeleteStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ArmNetworkInterfaceDeleteStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "armNetworkInterfaceDeleteStatusCheckerTask";

    private NetworkInterfaceCheckerContext networkInterfaceCheckerContext;
    private ArmClient armClient;

    public ArmNetworkInterfaceDeleteStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient, NetworkInterfaceCheckerContext
            networkInterfaceCheckerContext) {
        super(authenticatedContext, false);
        this.networkInterfaceCheckerContext = networkInterfaceCheckerContext;
        this.armClient = armClient;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.getClient(networkInterfaceCheckerContext.getArmCredentialView());
        try {
            client.getNetworkInterface(networkInterfaceCheckerContext.getGroupName(), networkInterfaceCheckerContext.getNetworkName());
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

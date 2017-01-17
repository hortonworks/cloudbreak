package com.sequenceiq.cloudbreak.cloud.arm.task;

import static com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.PublicIpCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

import groovyx.net.http.HttpResponseException;

@Component(ArmPublicIpDeleteStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ArmPublicIpDeleteStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "armPublicIpDeleteStatusCheckerTask";

    private final PublicIpCheckerContext ipCheckerContext;

    private final ArmClient armClient;

    public ArmPublicIpDeleteStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient, PublicIpCheckerContext ipCheckerContext) {
        super(authenticatedContext, false);
        this.ipCheckerContext = ipCheckerContext;
        this.armClient = armClient;
    }

    @Override
    public Boolean call() {
        AzureRMClient client = armClient.getClient(ipCheckerContext.getArmCredentialView());
        try {
            client.getPublicIpAddress(ipCheckerContext.getGroupName(), ipCheckerContext.getAddressName());
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

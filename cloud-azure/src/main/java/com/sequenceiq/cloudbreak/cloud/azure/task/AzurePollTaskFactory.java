package com.sequenceiq.cloudbreak.cloud.azure.task;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.context.AzureInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface.NetworkInterfaceDetachChecker;
import com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface.NetworkInterfaceDetachCheckerContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class AzurePollTaskFactory {
    @Inject
    private ApplicationContext applicationContext;

    public PollTask<Boolean> interactiveLoginStatusCheckerTask(CloudContext cloudContext,
            AzureInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext) {
        return createPollTask(AzureInteractiveLoginStatusCheckerTask.NAME, cloudContext, armInteractiveLoginStatusCheckerContext);
    }

    public PollTask<Boolean> networkInterfaceDetachCheckerTask(AuthenticatedContext authenticatedContext,
            NetworkInterfaceDetachCheckerContext networkInterfaceDetachCheckerContext) {
        return createPollTask(NetworkInterfaceDetachChecker.NAME, authenticatedContext, networkInterfaceDetachCheckerContext);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}

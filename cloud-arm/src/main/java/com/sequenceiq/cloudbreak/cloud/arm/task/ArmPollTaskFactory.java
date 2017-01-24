package com.sequenceiq.cloudbreak.cloud.arm.task;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.arm.ArmClient;
import com.sequenceiq.cloudbreak.cloud.arm.context.ArmInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.NetworkInterfaceCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.PublicIpCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.ResourceGroupCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext;
import com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin.ArmInteractiveLoginStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Component
public class ArmPollTaskFactory {
    @Inject
    private ApplicationContext applicationContext;

    public PollTask<Boolean> newNetworkInterfaceDeleteStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient,
            NetworkInterfaceCheckerContext networkInterfaceCheckerContext) {
        return createPollTask(ArmNetworkInterfaceDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, networkInterfaceCheckerContext);
    }

    public PollTask<Boolean> newPublicIpDeleteStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient,
            PublicIpCheckerContext publicIpCheckerContext) {
        return createPollTask(ArmPublicIpDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, publicIpCheckerContext);
    }

    public PollTask<Boolean> newResourceGroupDeleteStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient, ResourceGroupCheckerContext
            resourceGroupDeleteCheckerContext) {
        return createPollTask(ArmResourceGroupDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, resourceGroupDeleteCheckerContext);
    }

    public PollTask<Boolean> newStorageStatusCheckerTask(AuthenticatedContext authenticatedContext, StorageCheckerContext
            storageCheckerContext) {
        return createPollTask(ArmStorageStatusCheckerTask.NAME, authenticatedContext, storageCheckerContext);
    }

    public PollTask<Boolean> newVirtualMachineDeleteStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient,
            VirtualMachineCheckerContext virtualMachineCheckerContext) {
        return createPollTask(ArmVirtualMachineDeleteStatusCheckerTask.NAME, authenticatedContext, armClient, virtualMachineCheckerContext);
    }

    public PollTask<Boolean> newVirtualMachineStatusCheckerTask(AuthenticatedContext authenticatedContext, ArmClient armClient, VirtualMachineCheckerContext
            virtualMachineCheckerContext) {
        return createPollTask(ArmVirtualMachineStatusCheckerTask.NAME, authenticatedContext, armClient, virtualMachineCheckerContext);
    }

    public PollTask<Boolean> interactiveLoginStatusCheckerTask(AuthenticatedContext authenticatedContext,
            ArmInteractiveLoginStatusCheckerContext armInteractiveLoginStatusCheckerContext) {
        return createPollTask(ArmInteractiveLoginStatusCheckerTask.NAME, authenticatedContext, armInteractiveLoginStatusCheckerContext);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}

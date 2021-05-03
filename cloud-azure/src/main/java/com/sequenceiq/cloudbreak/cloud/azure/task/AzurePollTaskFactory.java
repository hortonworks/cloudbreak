package com.sequenceiq.cloudbreak.cloud.azure.task;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureInteractiveLoginStatusCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerTask;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerTask;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.image.AzureManagedImageCreationCheckerTask;
import com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface.NetworkInterfaceDetachChecker;
import com.sequenceiq.cloudbreak.cloud.azure.task.networkinterface.NetworkInterfaceDetachCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount.StorageAccountChecker;
import com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount.StorageAccountCheckerContext;
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

    public PollTask<Boolean> storageAccountCheckerTask(AuthenticatedContext authenticatedContext,
            StorageAccountCheckerContext storageAccountCheckerContext) {
        return createPollTask(StorageAccountChecker.NAME, authenticatedContext, storageAccountCheckerContext);
    }

    public PollTask<Boolean> managedImageCreationCheckerTask(AuthenticatedContext authenticatedContext,
            AzureManagedImageCreationCheckerContext azureManagedImageCreationCheckerContext) {
        return createPollTask(AzureManagedImageCreationCheckerTask.NAME, authenticatedContext, azureManagedImageCreationCheckerContext);
    }

    public PollTask<Boolean> dnsZoneCreationCheckerTask(AuthenticatedContext authenticatedContext,
            AzureDnsZoneCreationCheckerContext azureDnsZoneCreationCheckerContext) {
        return createPollTask(AzureDnsZoneCreationCheckerTask.NAME, authenticatedContext, azureDnsZoneCreationCheckerContext);
    }

    public PollTask<DiskEncryptionSetInner> diskEncryptionSetCreationCheckerTask(AuthenticatedContext authenticatedContext,
            DiskEncryptionSetCreationCheckerContext checkerContext) {
        return createPollTask(DiskEncryptionSetCreationCheckerTask.NAME, authenticatedContext, checkerContext);
    }

    @SuppressWarnings("unchecked")
    private <T> T createPollTask(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
    }
}

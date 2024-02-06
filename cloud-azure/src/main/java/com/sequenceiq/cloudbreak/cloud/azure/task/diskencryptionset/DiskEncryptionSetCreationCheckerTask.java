package com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.azure.core.management.ProxyResource;
import com.azure.resourcemanager.compute.fluent.models.DiskEncryptionSetInner;
import com.azure.resourcemanager.compute.models.EncryptionSetIdentity;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollPredicateStateTask;

@Component(DiskEncryptionSetCreationCheckerTask.NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DiskEncryptionSetCreationCheckerTask extends PollPredicateStateTask<DiskEncryptionSetInner> {

    public static final String NAME = "DiskEncryptionSetCreationCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskEncryptionSetCreationCheckerTask.class);

    private final AzureClient azureClient;

    private final DiskEncryptionSetCreationCheckerContext checkerContext;

    public DiskEncryptionSetCreationCheckerTask(AuthenticatedContext authenticatedContext, DiskEncryptionSetCreationCheckerContext checkerContext) {
        super(requireNonNull(authenticatedContext), false, des -> isDiskEncryptionSetValid(des, checkerContext));
        azureClient = requireNonNull(authenticatedContext.getParameter(AzureClient.class));
        this.checkerContext = requireNonNull(checkerContext);
    }

    private static boolean isDiskEncryptionSetValid(DiskEncryptionSetInner des, DiskEncryptionSetCreationCheckerContext checkerContext) {
        Optional<DiskEncryptionSetInner> desOptional = Optional.ofNullable(des);
        String id = desOptional.map(ProxyResource::id)
                .orElse(null);
        String principalObjectId = desOptional.map(DiskEncryptionSetInner::identity)
                .map(EncryptionSetIdentity::principalId)
                .orElse(null);
        boolean principalObjectPresent = principalObjectId != null;
        if (checkerContext.isUserManagedIdentityEnabled()) {
            principalObjectPresent = true;
        }
        return id != null && principalObjectPresent;
    }

    @Override
    protected DiskEncryptionSetInner doCall() {
        String resourceGroupName = checkerContext.getResourceGroupName();
        String diskEncryptionSetName = checkerContext.getDiskEncryptionSetName();
        LOGGER.info("Waiting for the creation of Disk Encryption Set \"{}\" in Resource Group \"{}\" to complete", diskEncryptionSetName, resourceGroupName);

        return azureClient.getDiskEncryptionSetByName(resourceGroupName, diskEncryptionSetName);
    }

}

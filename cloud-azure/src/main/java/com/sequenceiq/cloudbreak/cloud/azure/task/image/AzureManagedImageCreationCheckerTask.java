package com.sequenceiq.cloudbreak.cloud.azure.task.image;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureManagedImageService;
import com.sequenceiq.cloudbreak.cloud.azure.task.storageaccount.StorageAccountChecker;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(AzureManagedImageCreationCheckerTask.NAME)
@Scope("prototype")
public class AzureManagedImageCreationCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "AzureManagedImageCreationChecker";

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageAccountChecker.class);

    @Inject
    private AzureManagedImageService azureManagedImageService;

    private final AzureManagedImageCreationCheckerContext context;

    public AzureManagedImageCreationCheckerTask(AuthenticatedContext authenticatedContext, AzureManagedImageCreationCheckerContext context) {
        super(authenticatedContext, false);
        this.context = context;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.info("Waiting for managed image to be created: {}", context.getAzureImageInfo().getImageNameWithRegion());
        Optional<VirtualMachineCustomImage> virtualMachineCustomImage = findVirtualMachineCustomImage();
        if (virtualMachineCustomImage.isPresent()) {
            LOGGER.info("Managed image creation has been finished.");
            return true;
        } else {
            LOGGER.info("Managed image creation not finished yet.");
            return false;
        }
    }

    private Optional<VirtualMachineCustomImage> findVirtualMachineCustomImage() {
        return azureManagedImageService.findVirtualMachineCustomImage(context.getAzureImageInfo(), context.getAzureClient());
    }
}

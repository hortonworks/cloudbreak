package com.sequenceiq.cloudbreak.cloud.task;


import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.domain.ImageStatusResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.domain.ImageStatus;

public class PollImageStateTask extends PollTask<ImageStatusResult> {

    private Setup setup;
    private CloudStack stack;

    @Inject
    public PollImageStateTask(AuthenticatedContext authenticatedContext, Setup setup, CloudStack stack, boolean cancellable) {
        super(authenticatedContext, cancellable);
        this.setup = setup;
        this.stack = stack;
    }

    @Override
    public ImageStatusResult call() throws Exception {
        return setup.checkImageStatus(getAuthenticatedContext(), stack);
    }

    @Override
    public boolean completed(ImageStatusResult imageStatusResult) {
        return ImageStatus.CREATE_FAILED.equals(imageStatusResult.getImageStatus()) || ImageStatus.CREATE_FINISHED.equals(imageStatusResult.getImageStatus());
    }
}

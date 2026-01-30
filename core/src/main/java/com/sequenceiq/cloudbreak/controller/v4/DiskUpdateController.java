package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskModificationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
@InternalOnly
public class DiskUpdateController implements DiskUpdateEndpoint {

    @Inject
    private DiskUpdateService diskUpdateService;

    @Override
    @AccountIdNotNeeded
    public Boolean isDiskTypeChangeSupported(String platform) {
        return diskUpdateService.isDiskTypeChangeSupported(platform);
    }

    @Override
    @AccountIdNotNeeded
    public FlowIdentifier updateDiskTypeAndSize(DiskModificationRequest diskModificationRequest) throws Exception {
        DiskUpdateRequest diskUpdateRequest = diskModificationRequest.getDiskUpdateRequest();
        List<Volume> volumesToUpdate = diskModificationRequest.getVolumesToUpdate();
        return diskUpdateService.resizeDisks(
                diskModificationRequest.getStackId(),
                diskUpdateRequest.getGroup(),
                diskUpdateRequest.getVolumeType(),
                diskUpdateRequest.getSize(),
                volumesToUpdate
        );
    }

    @Override
    @AccountIdNotNeeded
    public void stopCMServices(long stackId) throws Exception {
        diskUpdateService.stopCMServices(stackId);
    }

    @Override
    @AccountIdNotNeeded
    public void startCMServices(long stackId) throws Exception {
        diskUpdateService.startCMServices(stackId);
    }
}

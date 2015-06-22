package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class EbsVolumeContext extends StackContext {

    private String volumeId;


    public EbsVolumeContext(Stack stack, String volumeId) {
        super(stack);
        this.volumeId = volumeId;
    }

    public String getVolumeId() {
        return volumeId;
    }

}

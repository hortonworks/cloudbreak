package com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CollectAdditionalHostnamesRequest extends StackEvent {

    public CollectAdditionalHostnamesRequest(Long stackId) {
        super(stackId);
    }
}

package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.cloud.model.Platform;

/**
 * Marker for flow context objects.
 * Flow context implementers are intended to encapsulate all the information required for specific phases of the flow.
 */
public interface FlowContext {

    Long getStackId();

    Platform getCloudPlatform();

}

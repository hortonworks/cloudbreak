package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.context.LightHouseInitContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class LightHouseRequest extends CloudPlatformRequest<LightHouseResult> {

    private LightHouseInitContext lightHouseInit;

    public LightHouseRequest(CloudContext cloudContext, ExtendedCloudCredential cloudCredential, LightHouseInitContext lightHouseInit) {
        super(cloudContext, cloudCredential);
        this.lightHouseInit = lightHouseInit;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return (ExtendedCloudCredential) getCloudCredential();
    }

    public LightHouseInitContext getLightHouseInit() {
        return lightHouseInit;
    }
}

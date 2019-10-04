package com.sequenceiq.datalake.service.sdx;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

import java.util.Objects;

public class DatabaseConfigKey {

    private final CloudPlatform cloudPlatform;

    private final SdxClusterShape sdxClusterShape;

    public DatabaseConfigKey(CloudPlatform cloudPlatform, SdxClusterShape sdxClusterShape) {
        this.cloudPlatform = Objects.requireNonNull(cloudPlatform, "cloudPlatform is null");
        this.sdxClusterShape = Objects.requireNonNull(sdxClusterShape, "sdxClusterShape is null");
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public SdxClusterShape getSdxClusterShape() {
        return sdxClusterShape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DatabaseConfigKey that = (DatabaseConfigKey) o;
        return cloudPlatform == that.cloudPlatform && sdxClusterShape == that.sdxClusterShape;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudPlatform, sdxClusterShape);
    }

}

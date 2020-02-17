package com.sequenceiq.datalake.service.sdx;

import java.util.Objects;
import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class CDPConfigKey {

    private final CloudPlatform cloudPlatform;

    private final SdxClusterShape clusterShape;

    private final String runtimeVersion;

    public CDPConfigKey(CloudPlatform cloudPlatform, SdxClusterShape clusterShape, String runtimeVersion) {
        this.cloudPlatform = cloudPlatform;
        this.clusterShape = clusterShape;
        this.runtimeVersion = runtimeVersion;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CDPConfigKey that = (CDPConfigKey) o;
        return cloudPlatform == that.cloudPlatform &&
                clusterShape == that.clusterShape &&
                runtimeVersion.equals(that.runtimeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudPlatform, clusterShape, runtimeVersion);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CDPConfigKey.class.getSimpleName() + "[", "]")
                .add("cloudPlatform=" + cloudPlatform)
                .add("clusterShape=" + clusterShape)
                .add("runtimeVersion='" + runtimeVersion + "'")
                .toString();
    }
}

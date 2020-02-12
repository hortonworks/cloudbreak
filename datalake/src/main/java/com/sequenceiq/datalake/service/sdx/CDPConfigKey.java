package com.sequenceiq.datalake.service.sdx;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class CDPConfigKey {

    private final CloudPlatform cloudPlatform;

    private final SdxClusterShape clusterShape;

    private final String cdpVersion;

    public CDPConfigKey(CloudPlatform cloudPlatform, SdxClusterShape clusterShape, String cdpVersion) {
        this.cloudPlatform = cloudPlatform;
        this.clusterShape = clusterShape;
        this.cdpVersion = cdpVersion;
    }

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public String getCdpVersion() {
        return cdpVersion;
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
                cdpVersion.equals(that.cdpVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudPlatform, clusterShape, cdpVersion);
    }
}

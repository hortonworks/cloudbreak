package com.sequenceiq.cloudbreak.service.java.vm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderOrEqualThanLimited;

import org.apache.commons.lang3.StringUtils;

public class JavaConfiguration {

    private int version;

    private String maxRuntimeVersion;

    private String minRuntimeVersion;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getMaxRuntimeVersion() {
        return maxRuntimeVersion;
    }

    public void setMaxRuntimeVersion(String maxRuntimeVersion) {
        this.maxRuntimeVersion = maxRuntimeVersion;
    }

    public String getMinRuntimeVersion() {
        return minRuntimeVersion;
    }

    public void setMinRuntimeVersion(String minRuntimeVersion) {
        this.minRuntimeVersion = minRuntimeVersion;
    }

    public boolean isRuntimeCompatible(String runtimeVersion) {
        if (StringUtils.isBlank(maxRuntimeVersion)) {
            if (isRuntimeVersionHigherThenTheMinimumAndNoMaximumDefined(runtimeVersion)) {
                return true;
            }
        } else if (isRuntimeVersionInTheCurrentVersionRange(runtimeVersion)) {
            return true;
        }
        return false;
    }

    private boolean isRuntimeVersionHigherThenTheMinimumAndNoMaximumDefined(String runtimeVersion) {
        return isVersionNewerOrEqualThanLimited(runtimeVersion, () -> this.minRuntimeVersion);
    }

    private boolean isRuntimeVersionInTheCurrentVersionRange(String runtimeVersion) {
        return isVersionNewerOrEqualThanLimited(runtimeVersion, () -> this.minRuntimeVersion) &&
                isVersionOlderOrEqualThanLimited(runtimeVersion, () -> this.maxRuntimeVersion);
    }
}

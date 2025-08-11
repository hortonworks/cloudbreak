package com.sequenceiq.cloudbreak.service.java.vm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderOrEqualThanLimited;

import org.apache.commons.lang3.StringUtils;

public class JavaConfiguration {

    private static final int VERSION_PARTS_WITHOUT_PATCH_NUMBER = 3;

    private static final int VERSION_PARTS_WITH_PATCH_NUMBER = 4;

    private int version;

    private String maxRuntimeVersion;

    private String minRuntimeVersion;

    public boolean isRuntimeCompatible(String runtimeVersion) {
        if (StringUtils.isBlank(maxRuntimeVersion)) {
            if (isMinPatchVersionConfiguredButMissingFromImage(runtimeVersion)) {
                return false;
            } else if (isRuntimeVersionHigherThanTheMinimumAndNoMaximumDefined(runtimeVersion)) {
                return true;
            }
        } else {
            if (isMinPatchVersionConfiguredButMissingFromImage(runtimeVersion) || isMaxPatchVersionConfiguredButMissingFromImage(runtimeVersion)) {
                return false;
            } else if (isRuntimeVersionInTheCurrentVersionRange(runtimeVersion)) {
                return true;
            }
        }
        return false;
    }

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

    private boolean isRuntimeVersionHigherThanTheMinimumAndNoMaximumDefined(String runtimeVersion) {
        return isVersionNewerOrEqualThanLimited(correctRuntimeVersion(runtimeVersion, minRuntimeVersion), () -> this.minRuntimeVersion);
    }

    private boolean isRuntimeVersionInTheCurrentVersionRange(String runtimeVersion) {
        return isVersionNewerOrEqualThanLimited(correctRuntimeVersion(runtimeVersion, minRuntimeVersion), () -> this.minRuntimeVersion) &&
                isVersionOlderOrEqualThanLimited(correctRuntimeVersion(runtimeVersion, maxRuntimeVersion), () -> this.maxRuntimeVersion);
    }

    private String correctRuntimeVersion(String runtimeVersion, String configVersion) {
        int runtimeParts = runtimeVersion.split("\\.").length;
        int configParts = configVersion.split("\\.").length;
        if (runtimeParts == VERSION_PARTS_WITH_PATCH_NUMBER && configParts == VERSION_PARTS_WITHOUT_PATCH_NUMBER) {
            // Remove the last digit from runtimeVersion
            return runtimeVersion.substring(0, runtimeVersion.lastIndexOf('.'));
        }
        return runtimeVersion;
    }

    private boolean isMinPatchVersionConfiguredButMissingFromImage(String runtimeVersion) {
        int runtimeParts = runtimeVersion.split("\\.").length;
        int minRuntimeParts = this.minRuntimeVersion.split("\\.").length;
        return runtimeParts == VERSION_PARTS_WITHOUT_PATCH_NUMBER && minRuntimeParts == VERSION_PARTS_WITH_PATCH_NUMBER;
    }

    private boolean isMaxPatchVersionConfiguredButMissingFromImage(String runtimeVersion) {
        int runtimeParts = runtimeVersion.split("\\.").length;
        int maxRuntimeParts = this.maxRuntimeVersion.split("\\.").length;
        return runtimeParts == VERSION_PARTS_WITHOUT_PATCH_NUMBER && maxRuntimeParts == VERSION_PARTS_WITH_PATCH_NUMBER;
    }
}

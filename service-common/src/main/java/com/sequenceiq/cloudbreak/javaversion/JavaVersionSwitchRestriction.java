package com.sequenceiq.cloudbreak.javaversion;

import static com.sequenceiq.cloudbreak.javaversion.RuntimeVersionRange.compareVersions;

import org.apache.commons.lang3.StringUtils;

public record JavaVersionSwitchRestriction(int jdk, String until, String reason) {

    public boolean blocks(String runtimeVersion) {
        return StringUtils.isBlank(until) || compareVersions(runtimeVersion, until) < 0;
    }

    @Override
    public String toString() {
        String scope = StringUtils.isBlank(until) ? " never switchable" : " not switchable until " + until;
        return "Java " + jdk + scope + " (" + reason + ")";
    }
}

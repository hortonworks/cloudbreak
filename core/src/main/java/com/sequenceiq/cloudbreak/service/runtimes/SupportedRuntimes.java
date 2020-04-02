package com.sequenceiq.cloudbreak.service.runtimes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;

@Component
public class SupportedRuntimes {

    // Defines what is the latest version what is supported, every former version shall just work
    @Value("${cb.runtimes.latest}")
    private String latestSupportedRuntime;

    private VersionComparator versionComparator = new VersionComparator();

    public boolean isSupported(String runtime) {
        boolean ret;
        if (Strings.isNullOrEmpty(latestSupportedRuntime)) {
            ret = true;
        } else {
            ret = versionComparator.compare(() -> runtime, () -> latestSupportedRuntime) <= 0;
        }
        return ret;
    }
}

package com.sequenceiq.cloudbreak.common.gov;

import java.util.Comparator;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Service
public class CommonGovService {

    public static final String GOV = "_gov";

    public boolean govCloudCompatibleVersion(String currentVersion) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> currentVersion, () -> "7.2.16") > -1;
    }

    public boolean govCloudDeployment(Set<String> enabledGovPlatforms, Set<String> enabledPlatforms) {
        String aws = CloudPlatform.AWS.name();
        boolean awsGovEnabled = enabledGovPlatforms.contains(aws);
        boolean awsEnabled = enabledPlatforms.contains(aws);
        return awsGovEnabled && !awsEnabled;
    }

}

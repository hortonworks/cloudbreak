package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AwsImdsService {

    private static final String IMDS_KEY = "imds";

    private static final String IMDS_V2 = "v2";

    @Inject
    private EntitlementService entitlementService;

    public boolean isImdsV2Supported(CloudStack cloudStack, String accountId) {
        if (entitlementService.isAwsImdsV2Enforced(accountId) && cloudStack != null && cloudStack.getImage() != null) {
            Map<String, String> packageVersions = MapUtils.emptyIfNull(cloudStack.getImage().getPackageVersions());
            return packageVersions.containsKey(IMDS_KEY) && StringUtils.equals(packageVersions.get(IMDS_KEY), IMDS_V2);
        }
        return false;
    }
}

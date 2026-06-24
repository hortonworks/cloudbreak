package com.sequenceiq.cloudbreak.cloud.model.view;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class PlatformResourceSecurityGroupFilterView {

    /**
     * Filter map key for comma-separated multi-ID lookup. When set, providers should look up
     * every ID in the set in a single call (falling back to per-ID calls where the SDK requires it)
     * and return whichever IDs exist; missing IDs must not fail the whole call. This is used by
     * {@code SecurityGroupValidationHandler} to distinguish missing from wrong-network SGs. Kept
     * separate from the legacy single-ID {@code "groupId"} key so existing callers are unaffected.
     */
    public static final String GROUP_IDS_KEY = "groupIds";

    private final String vpcId;

    private final String groupName;

    private final String groupId;

    private final Set<String> groupIds;

    public PlatformResourceSecurityGroupFilterView(Map<String, String> filters) {
        vpcId = filters.get("vpcId");
        groupName = filters.get("groupName");
        groupId = filters.get("groupId");
        groupIds = parseGroupIds(filters.get(GROUP_IDS_KEY));
    }

    public String getVpcId() {
        return vpcId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    private Set<String> parseGroupIds(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Set.of();
        } else {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
        }
    }
}

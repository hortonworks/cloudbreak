package com.sequenceiq.cloudbreak.template.views;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import java.util.Map;

public class AccountMappingView {

    public static final AccountMappingView EMPTY_MAPPING = new AccountMappingView(null, null, Map.of());

    private final Map<String, String> groupMappings;

    private final Map<String, String> userMappings;

    private final Map<String, String> razMappings;

    public AccountMappingView(Map<String, String> groupMappings, Map<String, String> userMappings, Map<String, String> razMappings) {
        this.groupMappings = groupMappings == null ? emptyMap() : unmodifiableMap(groupMappings);
        this.userMappings = userMappings == null ? emptyMap() : unmodifiableMap(userMappings);
        this.razMappings = razMappings == null ? emptyMap() : unmodifiableMap(razMappings);
    }

    public Map<String, String> getGroupMappings() {
        return groupMappings;
    }

    public Map<String, String> getUserMappings() {
        return userMappings;
    }

    public Map<String, String> getRazMappings() {
        return razMappings;
    }
}

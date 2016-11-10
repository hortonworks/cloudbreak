package com.sequenceiq.cloudbreak.cloud.gcp.model;

import java.util.HashMap;
import java.util.Map;

public class MachineDefinitionView {

    private static final Integer LIMIT = 24;

    private Map<String, Object> map = new HashMap();

    public MachineDefinitionView(Map map) {
        this.map = map;
    }

    private String getParameter(String key) {
        return map.get(key) == null ? "" : String.valueOf(map.get(key));
    }

    public String getKind() {
        return getParameter("kind");
    }

    public String getId() {
        return getParameter("id");
    }

    public String getCreationTimestamp() {
        return getParameter("creationTimestamp");
    }

    public String getName() {
        return getParameter("name");
    }

    public String getDescription() {
        return getParameter("description");
    }

    public String getGuestCpus() {
        return getParameter("guestCpus");
    }

    public String getMemoryMb() {
        return getParameter("memoryMb");
    }

    public String getMaximumPersistentDisks() {
        return getParameter("maximumPersistentDisks");
    }

    public Integer getMaximumNumberWithLimit() {
        int maxNumber = Integer.valueOf(getMaximumPersistentDisks());
        return maxNumber > LIMIT ? LIMIT : maxNumber;
    }

    public String getMaximumPersistentDisksSizeGb() {
        return getParameter("maximumPersistentDisksSizeGb");
    }

    public String getZone() {
        return getParameter("zone");
    }

    public String getSelfLink() {
        return getParameter("selfLink");
    }

    public String getPrice() {
        return getParameter("price");
    }
}

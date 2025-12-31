package com.sequenceiq.common.api.type;

import java.util.Locale;

public enum CdpResourceType {
    DATAHUB,
    DATALAKE,
    FREEIPA,
    DATABASE,
    DEFAULT;

    public static CdpResourceType fromStackType(String type) {
        CdpResourceType cdpResourceType = DEFAULT;
        if (type == null || type.isEmpty()) {
            cdpResourceType = DEFAULT;
        } else if (type.toLowerCase(Locale.ROOT).contains("distrox") || type.toLowerCase(Locale.ROOT).contains("workload")) {
            cdpResourceType = DATAHUB;
        } else if (type.toLowerCase(Locale.ROOT).contains("sdx") || type.toLowerCase(Locale.ROOT).contains("datalake")) {
            cdpResourceType = DATALAKE;
        }
        return cdpResourceType;
    }
}

package com.sequenceiq.common.api.type;

import java.util.Locale;

public enum CdpResourceType {
    DATAHUB,
    DATALAKE,
    DEFAULT;

    public static CdpResourceType fromStackType(String type) {
        CdpResourceType cdpResourceType = DEFAULT;
        if (type == null || type.isEmpty()) {
            cdpResourceType = DEFAULT;
        } else if (type.toLowerCase(Locale.ROOT).contains("ditrox") || type.toLowerCase().contains("workload")) {
            cdpResourceType = DATAHUB;
        } else if (type.toLowerCase(Locale.ROOT).contains("sdx") || type.toLowerCase().contains("datalake")) {
            cdpResourceType = DATALAKE;
        }
        return cdpResourceType;
    }
}

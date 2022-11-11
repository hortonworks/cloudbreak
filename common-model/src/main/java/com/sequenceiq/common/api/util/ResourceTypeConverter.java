package com.sequenceiq.common.api.util;

public class ResourceTypeConverter {

    public static final String WORKLOAD = "WORKLOAD";

    public static final String DATAHUB = "DATAHUB";

    public static final String DATALAKE = "DATALAKE";

    public static final String SDX = "SDX";

    public static final String REDBEAMS = "REDBEAMS";

    public static final String FREEIPA = "FREEIPA";

    private ResourceTypeConverter() {

    }

    public static String convertToHumanReadableName(String resource) {
        switch (resource) {
            case DATAHUB:
            case WORKLOAD:
                return "Data Hub";
            case DATALAKE:
            case SDX:
                return "Data Lake";
            case REDBEAMS:
                return "External Database";
            case FREEIPA:
                return "FreeIPA";
            default:
                return resource;
        }
    }
}

package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistroxEnabledInstanceTypes {
    private static final String ENABLED_TYPES = " e2-standard-2," +
            "e2-standard-4," +
            "e2-standard-8," +
            "e2-standard-16," +
            "e2-standard-32," +
            "n2-standard-2," +
            "n2-standard-4," +
            "n2-standard-8," +
            "n2-standard-16," +
            "n2-standard-32," +
            "n2-standard-48," +
            "n2-standard-64," +
            "n2-standard-80," +
            "n1-standard-2," +
            "n1-standard-4," +
            "n1-standard-8," +
            "n1-standard-16," +
            "n1-standard-32," +
            "n1-standard-48," +
            "n1-standard-64," +
            "n1-standard-96," +
            "n2d-standard-2," +
            "n2d-standard-4," +
            "n2d-standard-8," +
            "n2d-standard-16," +
            "n2d-standard-32," +
            "n2d-standard-48," +
            "n2d-standard-64," +
            "n2d-standard-80," +
            "n2d-highmem-16," +
            "n2d-highcpu-8," +
            "n2d-highcpu-16," +
            "n2d-highcpu-32," +
            "n2d-highmem-32," +
            "n2d-highmem-64," +
            "e2-highmem-8," +
            "e2-highmem-16," +
            "e2-highcpu-8," +
            "e2-highcpu-16," +
            "e2-highcpu-32";

    public static final List<String> GCP_ENABLED_TYPES_LIST = new ArrayList<String>(Arrays.asList(ENABLED_TYPES.trim().split(",")));

    private DistroxEnabledInstanceTypes() {
    }
}

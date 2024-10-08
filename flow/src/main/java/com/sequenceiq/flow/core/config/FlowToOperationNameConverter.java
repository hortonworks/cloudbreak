package com.sequenceiq.flow.core.config;

public class FlowToOperationNameConverter {

    private FlowToOperationNameConverter() {
    }

    public static String toOperationName(String value) {
        if (value.contains("Sdx")) {
            return value.replace("Sdx", "DataLake");
        } else if (value.contains("Datalake")) {
            return value.replace("Datalake", "DataLake");
        } else if (value.startsWith("Env") && !value.startsWith("Environment")) {
            return value.replaceFirst("Env", "Environment");
        } else if (value.contains("Datahub")) {
            return value.replace("Datahub", "DataHub");
        } else if (value.contains("Distrox")) {
            return value.replace("Distrox", "DataHub");
        } else if (value.contains("DistroX")) {
            return value.replace("DistroX", "DataHub");
        }
        return value;
    }
}

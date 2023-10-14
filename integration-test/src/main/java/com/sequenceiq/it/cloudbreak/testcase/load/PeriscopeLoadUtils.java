package com.sequenceiq.it.cloudbreak.testcase.load;

public class PeriscopeLoadUtils {

    public static final String USER_CRN_PATTERN = "crn:altus:iam:us-west-1:tenant-%d:user:loaduser@cloudera.com";

    public static final String CREDENTIAL_NAME_PATTERN = "tenant-%d-credential";

    public static final String CATALOG_NAME_PATTERN = "tenant-%d-catalog";

    public static final String BLUEPRINT_NAME_PATTERN = "tenant-%d-blueprint";

    public static final String IMAGE_NAME_PATTERN = "tenant-%d-image";

    public static final String ENVIRONMENT_NAME_PATTERN = "tenant-%d-env-%d";

    public static final String FREEIPA_NAME_PATTERN = "%s-freeipa";

    public static final String DATALAKE_NAME_PATTERN = "%s-dl";

    public static final String DATAHUB_NAME_PATTERN = "%s-dh-%d";

    public static final String CLUSTER_NAME_PATTERN = "%s-cluster";

    public static final String LOAD_ALERT_NAME_PATTERN = "as-%s";

    private PeriscopeLoadUtils() {

    }

    public static String getUserCrn(int tenantIndex) {
        return String.format(USER_CRN_PATTERN, tenantIndex);
    }

    public static String getCredentialName(int tenantIndex) {
        return String.format(CREDENTIAL_NAME_PATTERN, tenantIndex);
    }

    public static String getCatalogName(int tenantIndex) {
        return String.format(CATALOG_NAME_PATTERN, tenantIndex);
    }

    public static String getBlueprintName(int tenantIndex) {
        return String.format(BLUEPRINT_NAME_PATTERN, tenantIndex);
    }

    public static String getImageName(int tenantIndex) {
        return String.format(IMAGE_NAME_PATTERN, tenantIndex);
    }

    public static String getEnvironmentName(int tenantIndex, int environmentIndex) {
        return String.format(ENVIRONMENT_NAME_PATTERN, tenantIndex, environmentIndex);
    }

    public static String getFreeIpaName(String envName) {
        return String.format(FREEIPA_NAME_PATTERN, envName);
    }

    public static String getDataLakeName(String envName) {
        return String.format(DATALAKE_NAME_PATTERN, envName);
    }

    public static String getDataHubName(String envName, int dataHubIndex) {
        return String.format(DATAHUB_NAME_PATTERN, envName, dataHubIndex);
    }

    public static String getClusterName(String dataHubName) {
        return String.format(CLUSTER_NAME_PATTERN, dataHubName);
    }

    public static String getLoadAlertName(String dataHubName) {
        return String.format(LOAD_ALERT_NAME_PATTERN, dataHubName);
    }
}

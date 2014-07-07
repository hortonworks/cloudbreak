package com.sequenceiq.cloudbreak.service.stack.azure;

public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    public static final String CREDENTIAL = "credential";
    public static final String EMAILASFOLDER = "emailAsFolder";

    private AzureStackUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Instantiate this class is not possible.");
    }

    public static String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }
}

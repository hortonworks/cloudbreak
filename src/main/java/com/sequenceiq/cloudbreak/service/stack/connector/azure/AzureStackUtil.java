package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    public static final String CREDENTIAL = "credential";
    public static final String EMAILASFOLDER = "emailAsFolder";
    public static final String IMAGE_NAME = "ambari-docker-v1";

    private AzureStackUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Instantiate this class is not possible.");
    }

    public static String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }

    public static String getOsImageName(Credential credential) {
        return String.format("%s-%s", ((AzureCredential) credential).getName().replaceAll("\\s+", ""), IMAGE_NAME);
    }
}

package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;

import okhttp3.JavaNetAuthenticator;

/**
 * In order to prevent leaking credential info in unit tests the tests use this class to get credentials.
 * This class, in turn, tries to read file $HOME/.cb-poc-creds/azure-creds
 *
 * To test if the credentials could be read please run {@link AzureTestCredentialsTest} class. Please note it does not check if supplied values are valid.
 *
 * Its format is as follows:
 *
 * azure.cred.tenant=[TENANT_ID]
 * azure.cred.subscription=[SUBSCRIPTION_ID]
 * azure.cred.client=[CLIENT_ID]
 * azure.cred.secret=[CLIENT_SECRET]
 * azure.storage.connectionstring=[STORAGE_ACCOUNT_CONNECTION_STRING]
 *
 */
public class AzureTestCredentials {

    public static final String AZURE_CREDS_FILE = ".cb-poc-creds/azure-creds";

    private Map<String, String> credentialMap = null;

    public ApplicationTokenCredentials getCredentials() {
        return new ApplicationTokenCredentials(
                getApplicationId(),
                getTenantId(),
                getClientSecret(),
                AzureEnvironment.AZURE);
    }

    public Azure getAzure() {
        return Azure
                .configure()
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(LogLevel.BODY_AND_HEADERS)
                .authenticate(getCredentials())
                .withSubscription(getSubscriptionId());

    }

    public String getTenantId() {
        return getVariableFromFile("azure.cred.tenant");
    }

    public String getApplicationId() {
        return getVariableFromFile("azure.cred.client");
    }

    public String getClientSecret() {
        return getVariableFromFile("azure.cred.secret");
    }

    public String getSubscriptionId() { return getVariableFromFile("azure.cred.subscription"); }

    public String getStorageAccountConnectionString() {
        return getVariableFromFile("azure.storage.connectionstring");
    }

    private String getVariableFromFile(String envVarName) {
        try {
            if (credentialMap == null) {
                String userHome = System.getenv("HOME");
                credentialMap = Files.readAllLines(Paths.get(userHome, AZURE_CREDS_FILE)).stream()
                        .map(l -> l.split("=", 2))
                        .collect(Collectors.toMap(a -> a[0], a -> String.join("", a[1])));
            }
            return Optional.ofNullable(credentialMap.get(envVarName))
                    .orElseThrow(() -> new RuntimeException(String.format("Env var %s not defined", envVarName)));
        } catch (IOException e) {
            throw new RuntimeException("Could not open " + AZURE_CREDS_FILE + ", please check the file exists", e);
        }
    }

}

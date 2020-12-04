package com.sequenceiq.cloudbreak.cloud.azure.image;

import java.net.MalformedURLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;

import okhttp3.JavaNetAuthenticator;

public class ImageCopyWithPutBlobRestApi {

    public static final String AZURE_PUBLIC_AUTHORITY = "https://login.microsoftonline.com/%s/";
    private static final String SCOPE = "https://storage.azure.com/.default";
    public static final String STORAGE_API_VERSION = "2019-12-12";
    private AzureTestCredentials azureTestCredentials = new AzureTestCredentials();
    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void runCopy() {
        Azure azure = Azure
                .configure()
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(LogLevel.BODY_AND_HEADERS)
                .authenticate(azureTestCredentials.getCredentials())
                .withSubscription("3ddda1c7-d1f5-4e7b-ac81-0523f483b3b3");

        String destResourceGroup = "rg-gpapp-single-rg";
        String destStorageName = "cbimgwu9d62091440e606d4";
        String destContainerName = "images";
        String sourceBlob = "https://cldrwestus.blob.core.windows.net/images/freeipa-cdh--2008121423.vhd";

        Optional<IAuthenticationResult> authenticationResultOptional = getAccessToken(azureTestCredentials);
        if (authenticationResultOptional.isPresent()) {
            String oauthToken = authenticationResultOptional.get().accessToken();
            copyImage(azure, destResourceGroup, destStorageName, destContainerName, sourceBlob, oauthToken);
//            getSourceBlobProperties(sourceBlob, oauthToken);
        }
    }

    private void copyImage(Azure azure, String resourceGroup, String storageName, String containerName, String sourceBlob, String oauthToken) {
        String url = String.format("https://%s.blob.core.windows.net/mycontainer/myblob", storageName);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("", "");
        HttpEntity httpEntity = new HttpEntity(httpHeaders);

        restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
    }

    private void getSourceBlobProperties(String sourceBlobUrl, String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.add("Authorization", "Bearer " + token);
//        httpHeaders.add("x-ms-date", "Wed, 03 Dec 2020 20:24:11 GMT");
//        httpHeaders.add("x-ms-date", getUtcTime());
        httpHeaders.add("x-ms-version", STORAGE_API_VERSION);
//        httpHeaders.add("x-ms-client-request-id", generateRequestId());
        HttpEntity httpEntity = new HttpEntity(httpHeaders);

//        try {
            ResponseEntity<Void> response = restTemplate.exchange(sourceBlobUrl, HttpMethod.HEAD, httpEntity, Void.class);
            HttpHeaders responseHeaders = response.getHeaders();
            System.out.println(responseHeaders);
//        } catch(HttpClientErrorException e) {
//            System.out.println(e);
//        }

    }

    private String getUtcTime() {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    private Optional<IAuthenticationResult> getAccessToken(AzureTestCredentials azureTestCredentials) {
        String authority = String.format(AZURE_PUBLIC_AUTHORITY, azureTestCredentials.getTenantId());
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                    azureTestCredentials.getApplicationId(),
                    ClientCredentialFactory.createFromSecret(azureTestCredentials.getClientSecret())
            )
                    .authority(authority)
                    .build();

            // With client credentials flows the scope is ALWAYS of the shape "resource/.default", as the
            // application permissions need to be set statically (in the portal), and then granted by a tenant administrator
            ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                    Collections.singleton(SCOPE))
                    .build();
            CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
            return Optional.of(future.get());
        } catch (MalformedURLException e) {
            System.out.println("error with URL");
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception: " + e);
        } catch (ExecutionException e) {
            System.out.println("Execution exception" + e);
        }
        return Optional.empty();
    }

    private String generateRequestId() {
        return "gpapp" + UUID.randomUUID();
    }
}
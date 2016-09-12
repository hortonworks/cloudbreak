package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public final class GcpStackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpStackUtil.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);
    private static final String GCP_IMAGE_TYPE_PREFIX = "https://www.googleapis.com/compute/v1/projects/%s/global/images/";
    private static final String EMPTY_BUCKET = "";
    private static final int FINISHED = 100;
    private static final int PRIVATE_ID_PART = 2;
    private static final String SERVICE_ACCOUNT = "serviceAccountId";
    private static final String PRIVATE_KEY = "serviceAccountPrivateKey";
    private static final String PROJECT_ID = "projectId";
    private static final String NETWORK_ID = "networkId";
    private static final String SUBNET_ID = "subnetId";
    private static final String NO_PUBLIC_IP = "noPublicIp";
    private static final String NO_FIREWALL_RULES = "noFirewallRules";

    private GcpStackUtil() {
    }

    public static Compute buildCompute(CloudCredential gcpCredential) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildCredential(gcpCredential, httpTransport);
            return new Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(gcpCredential.getName())
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while building Google Compute access.", e);
        }
        return null;
    }

    public static GoogleCredential buildCredential(CloudCredential gcpCredential, HttpTransport httpTransport) throws IOException, GeneralSecurityException {
        PrivateKey pk = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                new ByteArrayInputStream(Base64.decodeBase64(getServiceAccountPrivateKey(gcpCredential))), "notasecret", "privatekey", "notasecret");
        return new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(getServiceAccountId(gcpCredential))
                .setServiceAccountScopes(SCOPES)
                .setServiceAccountPrivateKey(pk)
                .build();
    }

    public static String getServiceAccountPrivateKey(CloudCredential credential) {
        return credential.getParameter(PRIVATE_KEY, String.class);
    }

    public static String getServiceAccountId(CloudCredential credential) {
        return credential.getParameter(SERVICE_ACCOUNT, String.class);
    }

    public static String getProjectId(CloudCredential credential) {
        return credential.getParameter(PROJECT_ID, String.class).toLowerCase().replaceAll("[^A-Za-z0-9 ]", "-");
    }

    public static boolean analyzeOperation(Operation operation) throws Exception {
        String errorMessage = checkForErrors(operation);
        if (errorMessage != null) {
            throw new Exception(errorMessage);
        } else {
            Integer progress = operation.getProgress();
            return progress == FINISHED;
        }
    }

    public static String checkForErrors(Operation operation) {
        if (operation == null) {
            LOGGER.error("Operation is null!");
            return null;
        }
        String msg = null;
        if (operation.getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.getError().getErrors() != null) {
                for (Operation.Error.Errors errors : operation.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            } else {
                LOGGER.debug("No errors found, Error: {}", operation.getError());
            }
        }
        if (operation.getHttpErrorStatusCode() != null) {
            msg += String.format(" HTTP error message: %s, HTTP error status code: %s", operation.getHttpErrorMessage(), operation.getHttpErrorStatusCode());
        }
        return msg;
    }

    public static Compute.GlobalOperations.Get globalOperations(Compute compute, String projectId, String operationName) throws IOException {
        return compute.globalOperations().get(projectId, operationName);
    }

    public static Compute.ZoneOperations.Get zoneOperations(Compute compute, String projectId, String operationName, AvailabilityZone region)
            throws IOException {
        return compute.zoneOperations().get(projectId, region.value(), operationName);
    }

    public static Compute.RegionOperations.Get regionOperations(Compute compute, String projectId, String operationName, Region region) throws IOException {
        return compute.regionOperations().get(projectId, region.value(), operationName);
    }

    public static Storage buildStorage(CloudCredential gcpCredential, String name) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildCredential(gcpCredential, httpTransport);
            return new Storage.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(name)
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error occurred while building Google Storage access.", e);
        }
        return null;
    }

    public static String getBucket(String image) {
        if (!StringUtils.isEmpty(image) && createParts(image).length > 1) {
            String[] parts = createParts(image);
            return StringUtils.join(ArrayUtils.remove(parts, parts.length - 1), "/");
        } else {
            LOGGER.warn("No bucket found in source image path.");
            return EMPTY_BUCKET;
        }
    }

    public static String getTarName(String image) {
        if (!StringUtils.isEmpty(image)) {
            String[] parts = createParts(image);
            return parts[parts.length - 1];
        } else {
            throw new GcpResourceException("Source image path environment variable is not well formed");
        }
    }

    public static String getImageName(String image) {
        return getTarName(image).replaceAll("(\\.tar|\\.zip|\\.gz|\\.gzip)", "").replaceAll("\\.", "-");
    }

    public static String getAmbariImage(String projectId, String image) {
        return String.format(GCP_IMAGE_TYPE_PREFIX + getImageName(image), projectId);
    }

    public static Long getPrivateId(String resourceName) {
        try {
            return Long.valueOf(resourceName.split("-")[PRIVATE_ID_PART]);
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Cannot determine the private id of GCP instance, name: " + resourceName);
            return null;
        } catch (Exception e) {
            LOGGER.warn("Cannot determine the private id of GCP instance, name: " + resourceName, e);
            return null;
        }
    }

    public static boolean isExistingNetwork(Network network) {
        return isNoneEmpty(getCustomNetworkId(network));
    }

    public static boolean newSubnetInExistingNetwork(Network network) {
        return isExistingNetwork(network) && isNoneEmpty(network.getSubnet().getCidr());
    }

    public static boolean newNetworkAndSubnet(Network network) {
        return !isExistingNetwork(network);
    }

    public static boolean legacyNetwork(Network network) {
        return isAnyEmpty(network.getSubnet().getCidr()) && isAnyEmpty(getSubnetId(network));
    }

    public static boolean isExistingSubnet(Network network) {
        return isNoneEmpty(getSubnetId(network));
    }

    public static String getCustomNetworkId(Network network) {
        return network.getStringParameter(NETWORK_ID);
    }

    public static String getSubnetId(Network network) {
        return network.getStringParameter(SUBNET_ID);
    }

    public static Boolean noPublicIp(Network network) {
        Boolean noPublicIp = network.getParameter(NO_PUBLIC_IP, Boolean.class);
        if (noPublicIp == null) {
            return Boolean.FALSE;
        }
        return noPublicIp;
    }

    public static Boolean noFirewallRules(Network network) {
        Boolean noFirewallRules = network.getParameter(NO_FIREWALL_RULES, Boolean.class);
        if (noFirewallRules == null) {
            return Boolean.FALSE;
        }
        return noFirewallRules;
    }

    public static String getClusterTag(CloudContext cloudContext) {
        return cloudContext.getName() + cloudContext.getId();
    }

    public static String getGroupClusterTag(CloudContext cloudContext, Group group) {
        return group.getName().toLowerCase().replaceAll("[^A-Za-z0-9 ]", "") + cloudContext.getId();
    }

    private static String[] createParts(String splittable) {
        return splittable.split("/");
    }

}

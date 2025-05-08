package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.GlobalOperations;
import com.google.api.services.compute.Compute.RegionOperations.Get;
import com.google.api.services.compute.Compute.ZoneOperations;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Operation.Error.Errors;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.OperationError;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.FileSystemType;

@Service
public class GcpStackUtil {

    public static final String NETWORK_ID = "networkId";

    public static final String NETWORK_IP_RANGE = "networkRange";

    public static final String SHARED_PROJECT_ID = "sharedProjectId";

    public static final String SERVICE_ACCOUNT = "serviceAccountId";

    public static final String PROJECT_ID = "projectId";

    public static final String NO_PUBLIC_IP = "noPublicIp";

    public static final String CUSTOM_AVAILABILITY_ZONE = "customAvailabilityZone";

    public static final String NO_FIREWALL_RULES = "noFirewallRules";

    public static final String REGION = "region";

    public static final String TOKEN_ERROR = "The specified key for service account does not exist in the %s project";

    public static final String AUTHORIZATION_ERROR = "We could not authorize the credential on google side: %s";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpStackUtil.class);

    private static final String GCP_IMAGE_TYPE_PREFIX = "https://www.googleapis.com/compute/v1/projects/%s/global/images/%s";

    private static final String EMPTY_BUCKET = "";

    private static final String EMPTY_PATH = "";

    private static final int FINISHED = 100;

    private static final Set<String> FINISHED_STATUS = Set.of("DONE");

    private static final int PRIVATE_ID_PART = 2;

    private static final int MIN_PATH_PARTS = 3;

    private static final int FIRST = 0;

    private static final String GCP_SUBNET_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/regions/%s/subnetworks/%s";

    private static final String GCP_NETWORK_FORMAT = "https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s";

    public String getServiceAccountId(CloudCredential credential) {
        return credential.getParameter(SERVICE_ACCOUNT, String.class);
    }

    public boolean isLegacyNetwork(Network network) {
        return isAnyEmpty(network.getSubnet().getCidr()) && isAnyEmpty(getSubnetId(network));
    }

    public String getProjectId(CloudCredential credential) {
        String projectId = credential.getParameter(PROJECT_ID, String.class);
        if (projectId == null) {
            throw new CredentialVerificationException("Missing Project Id from GCP Credential");
        }
        return projectId.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9 ]", "-");
    }

    public boolean isNewNetworkAndSubnet(Network network) {
        return !isExistingNetwork(network);
    }

    public boolean isOperationFinished(Operation operation) throws Exception {
        String errorMessage = checkForErrors(operation);
        if (errorMessage != null) {
            throw new Exception(errorMessage);
        } else {
            Integer progress = operation.getProgress();
            return FINISHED == progress;
        }
    }

    public boolean isOperationFinished(com.google.api.services.sqladmin.model.Operation operation) throws Exception {
        String errorMessage = checkForErrors(operation);
        if (errorMessage != null) {
            throw new Exception(errorMessage);
        } else {
            String operationStatus = operation.getStatus();
            return FINISHED_STATUS.contains(operationStatus);
        }
    }

    private String checkForErrors(Operation operation) {
        if (operation == null) {
            LOGGER.info("Operation is null!");
            return null;
        }
        String msg = null;
        if (operation.getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.getError().getErrors() != null) {
                for (Errors errors : operation.getError().getErrors()) {
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

    private String checkForErrors(com.google.api.services.sqladmin.model.Operation operation) {
        if (operation == null) {
            LOGGER.info("Operation is null!");
            return null;
        }
        String msg = null;
        if (operation.getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.getError().getErrors() != null) {
                for (OperationError errors : operation.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            } else {
                LOGGER.debug("No errors found, Error: {}", operation.getError());
            }
        }
        return msg;
    }

    public GlobalOperations.Get globalOperations(Compute compute, String projectId, String operationName) throws IOException {
        return compute.globalOperations().get(projectId, operationName);
    }

    public SQLAdmin.Operations.Get sqlAdminOperations(SQLAdmin sqlAdmin, String projectId, String operationName) throws IOException {
        return sqlAdmin.operations().get(projectId, operationName);
    }

    public ZoneOperations.Get zoneOperation(Compute compute, String projectId, String operationName, String availabilityZone)
            throws IOException {
        return compute.zoneOperations().get(projectId, availabilityZone, operationName);
    }

    public ZoneOperations.List zoneOperationsByNames(Compute compute, String projectId, Set<String> operationNames, String availabilityZone)
            throws IOException {
        String filterWithOperationNames = operationNames.stream().map(opName -> String.format("name=%s", opName)).collect(Collectors.joining(" OR "));
        return compute.zoneOperations().list(projectId, availabilityZone)
                .setFilter(filterWithOperationNames)
                .setReturnPartialSuccess(Boolean.TRUE);
    }

    public Get regionOperations(Compute compute, String projectId, String operationName, Region region) throws IOException {
        return compute.regionOperations().get(projectId, region.value(), operationName);
    }

    public HttpRequestInitializer setHttpTimeout(HttpRequestInitializer requestInitializer) {
        return httpRequest -> {
            requestInitializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(GcpRequestTimeout.getTimeout());
            httpRequest.setReadTimeout(GcpRequestTimeout.getTimeout());
        };
    }

    public String getBucket(String image) {
        if (!StringUtils.isEmpty(image) && createParts(image).length > 1) {
            String[] parts = createParts(image.replaceAll("https://storage.googleapis.com/", ""));
            return StringUtils.join(ArrayUtils.remove(parts, parts.length - 1), "/");
        } else {
            LOGGER.debug("No bucket found in source image path.");
            return EMPTY_BUCKET;
        }
    }

    public String getTarName(String image) {
        if (!StringUtils.isEmpty(image)) {
            String[] parts = createParts(image);
            return parts[parts.length - 1];
        } else {
            throw new GcpResourceException("Source image path environment variable is not well formed");
        }
    }

    public String getBucketName(String objectStorageLocation) {
        String[] parts = createParts(objectStorageLocation.replaceAll(FileSystemType.GCS.getProtocol() + "://", ""));
        if (!StringUtils.isEmpty(objectStorageLocation) && parts.length >= 1) {
            return parts[FIRST];
        } else {
            LOGGER.debug("No bucket found in object storage location.");
            return EMPTY_BUCKET;
        }
    }

    public String getPath(String objectStorageLocation) {
        String[] parts = createParts(objectStorageLocation);
        if (!StringUtils.isEmpty(objectStorageLocation) && parts.length > MIN_PATH_PARTS) {
            return StringUtils.join(ArrayUtils.removeAll(parts, 0, 1), "/");
        } else {
            LOGGER.debug("No path found in object storage location.");
            return EMPTY_PATH;
        }
    }

    public String getImageName(String image) {
        if (image.contains("/")) {
            return getTarName(image).replaceAll("(\\.tar|\\.zip|\\.gz|\\.gzip)", "").replaceAll("\\.", "-");
        }
        return image.trim();
    }

    public String getCDPImage(String projectId, String image) {
        return String.format(GCP_IMAGE_TYPE_PREFIX, projectId, getImageName(image));
    }

    public Long getPrivateId(String resourceName) {
        try {
            return Long.valueOf(resourceName.split("-")[PRIVATE_ID_PART]);
        } catch (RuntimeException e) {
            LOGGER.warn("Cannot determine the private id of GCP instance, name: " + resourceName, e);
        }
        return null;
    }

    public boolean isExistingNetwork(Network network) {
        return isNotEmpty(getCustomNetworkId(network));
    }

    public boolean isNewSubnetInExistingNetwork(Network network) {
        return isExistingNetwork(network) && !isExistingSubnet(network);
    }

    public boolean isExistingSubnet(Network network) {
        return isNotEmpty(getSubnetId(network));
    }

    public String getCustomNetworkId(Network network) {
        return network.getStringParameter(NETWORK_ID);
    }

    public String getSubnetId(Network network) {
        return network.getStringParameter(SUBNET_ID);
    }

    public String getEndpointGatewaySubnetId(Network network) {
        return network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID);
    }

    public String getSubnetIds(Network network) {
        return network.getStringParameter(SUBNET_IDS);
    }

    public String getSharedProjectId(Network network) {
        return network.getStringParameter(SHARED_PROJECT_ID);
    }

    public Boolean noPublicIp(Network network) {
        Boolean noPublicIp = network.getParameter(NO_PUBLIC_IP, Boolean.class);
        if (noPublicIp == null) {
            return Boolean.FALSE;
        }
        return noPublicIp;
    }

    public String getSubnetUrl(String sharedProjectId, String value, String customSubnetworkId) {
        return String.format(GCP_SUBNET_FORMAT, sharedProjectId, value, customSubnetworkId);
    }

    public String getNetworkUrl(String projectId, String networkId) {
        return String.format(GCP_NETWORK_FORMAT, projectId, networkId);
    }

    public GcpResourceException getMissingServiceAccountKeyError(TokenResponseException e, String projectId) {
        if (e.getDetails() != null) {
            if (e.getDetails().getError() != null) {
                if (e.getDetails().getError().equals("invalid_grant")) {
                    String format = String.format(TOKEN_ERROR, projectId);
                    LOGGER.info(format, e);
                    return new GcpResourceException(format);
                } else {
                    LOGGER.info(e.getStatusMessage());
                    return new GcpResourceException(String.format(AUTHORIZATION_ERROR,
                            e.getDetails().getError()));
                }
            } else if (e.getDetails().getErrorDescription() != null) {
                String format = String.format(AUTHORIZATION_ERROR, e.getDetails().getErrorDescription());
                LOGGER.info(format, e);
                return new GcpResourceException(format);
            } else if (e.getDetails().getErrorUri() != null) {
                String format = String.format(AUTHORIZATION_ERROR, e.getDetails().getErrorDescription());
                LOGGER.info(format, e);
                return new GcpResourceException(format);
            }
        }
        String format = String.format(AUTHORIZATION_ERROR, e.getMessage());
        LOGGER.info(format, e);
        return new GcpResourceException(format);
    }

    public Boolean noFirewallRules(Network network) {
        Boolean noFirewallRules = network.getParameter(NO_FIREWALL_RULES, Boolean.class);
        if (noFirewallRules == null) {
            return Boolean.FALSE;
        }
        return noFirewallRules;
    }

    public String getClusterTag(CloudContext cloudContext) {
        return cloudContext.getName() + cloudContext.getId();
    }

    public String convertGroupName(String group) {
        return group.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9 ]", "");
    }

    public String getNetworkSecurityIdFromGroup(Group group) {
        // GCP firewall rules' target tags need to be added to the network tags for the firewall rule to take effect
        if (group.getSecurity() != null && group.getSecurity().getCloudSecurityId() != null) {
            return group.getSecurity().getCloudSecurityId();
        }
        return "";
    }

    public String getGroupClusterTag(CloudContext cloudContext, Group group) {
        return getGroupClusterTag(cloudContext, group.getName());
    }

    public String getGroupClusterTag(CloudContext cloudContext, String groupName) {
        return groupName.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9 ]", "") + cloudContext.getId();
    }

    public String getGroupTypeTag(InstanceGroupType type) {
        if (type == null) {
            throw new CloudbreakServiceException("Type of the group must not be null");
        }
        return type.name().toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9 ]", "");
    }

    public Instance getComputeInstanceWithId(Compute compute, String projectId, String availabilityZone, String instanceId) throws IOException {
        return compute.instances().get(projectId, availabilityZone, instanceId).execute();
    }

    private String[] createParts(String splittable) {
        return splittable.split("/");
    }

}

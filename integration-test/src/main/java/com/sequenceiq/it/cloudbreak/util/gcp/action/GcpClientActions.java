package com.sequenceiq.it.cloudbreak.util.gcp.action;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.gax.paging.Page;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.GlobalOperations;
import com.google.api.services.compute.Compute.ZoneOperations.Get;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.Storage.BlobSourceOption;
import com.google.cloud.storage.StorageException;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.gcp.client.GcpClient;

@Component
public class GcpClientActions extends GcpClient {
    protected static final int TIMEOUT = 600000;

    protected static final int POLLING_INTERVAL = 10000;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpClientActions.class);

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        LOGGER.info("Collect tags/labels for instance ids: '{}'", String.join(", ", instanceIds));
        Map<String, Map<String, String>> instanceTags = new HashMap<>();
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Instance instance = compute
                        .instances()
                        .get(getProjectId(), getAvailabilityZone(), instanceId)
                        .execute();
                instanceTags.put(instanceId, instance.getLabels());
            } catch (Exception e) {
                LOGGER.warn(format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
        return instanceTags;
    }

    public List<String> listInstanceTypes(List<String> instanceIds) {
        Compute compute = buildCompute();
        return instanceIds.stream().map(id -> {
            Instance instance = null;
            try {
                instance = compute.instances().get(getProjectId(), getAvailabilityZone(), id).execute();
            } catch (IOException e) {

            }
            String machineType = instance.getMachineType();
            if (machineType != null) {
                machineType = machineType.substring(machineType.lastIndexOf('/') + 1);
            }
            return machineType;
        }).collect(Collectors.toList());
    }

    private Map<String, Set<String>> listInstancesDiskNames(List<String> instanceIds) {
        LOGGER.info("Collect disk names for instance ids: '{}'", String.join(", ", instanceIds));
        Map<String, Set<String>> instanceIdDiskNamesMap = new HashMap<>();
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Instance instance = compute
                        .instances()
                        .get(getProjectId(), getAvailabilityZone(), instanceId)
                        .execute();
                Set<String> attachedDiskNames = instance
                        .getDisks()
                        .stream()
                        .filter(ad -> !ad.getBoot())
                        .map(AttachedDisk::getDeviceName)
                        .collect(Collectors.toSet());
                instanceIdDiskNamesMap.put(instanceId, attachedDiskNames);
            } catch (Exception e) {
                LOGGER.warn(format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
        instanceIdDiskNamesMap.forEach((instanceId, diskNames) -> Log.log(LOGGER, format(" Attached disk names are %s for [%s] instance ",
                diskNames.toString(), instanceId)));
        return instanceIdDiskNamesMap;
    }

    public List<String> getSelectedInstancesDiskNames(List<String> instanceIds) {
        Map<String, Set<String>> instanceIdDiskNamesMap = listInstancesDiskNames(instanceIds);
        return instanceIdDiskNamesMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Map<String, Set<String>> getInstanceDiskNames(String instanceId) {
        return listInstancesDiskNames(List.of(instanceId));
    }

    public List<String> listVolumeEncryptionKey(List<String> instanceIds) {
        List<String> encryptionKeys = new ArrayList<>();
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Instance instance = compute
                        .instances()
                        .get(getProjectId(), getAvailabilityZone(), instanceId)
                        .execute();
                Optional<String> encryptionKey = instance
                        .getDisks()
                        .stream().findFirst()
                        .map(AttachedDisk::getDiskEncryptionKey)
                        .map(CustomerEncryptionKey::getKmsKeyName);
                if (encryptionKey.isPresent()) {
                    encryptionKeys.add(encryptionKey.get());
                }
            } catch (Exception e) {
                LOGGER.warn(format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
        return encryptionKeys;
    }

    public Map<String, String> instanceSubnet(List<String> instanceIds) {
        return null;
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        Map<String, String> instancesWithAz = instanceIds.stream().collect(
                Collectors.toMap(Function.identity(), instance -> getAvailabilityZone()));
        deleteHostGroupInstancesWithAz(instancesWithAz);
    }

    public void deleteHostGroupInstancesWithAz(Map<String, String> instancesWithAz) {
        LOGGER.info("Deleting instances: '{}'", String.join(", ", instancesWithAz.keySet()));
        Compute compute = buildCompute();
        for (Map.Entry<String, String> instance : instancesWithAz.entrySet()) {
            String instanceId = instance.getKey();
            String availabilityZone = instance.getValue();
            try {
                Operation deleteOperation = compute
                        .instances()
                        .delete(getProjectId(), availabilityZone, instanceId)
                        .execute();
                Log.log(LOGGER, format(" Gcp instance [%s] state is [%s] with message: %s", instanceId, deleteOperation.getStatus(),
                        deleteOperation.getStatusMessage()));
                try {
                    deleteOperation = waitForComplete(compute, deleteOperation, getProjectId(), TIMEOUT);
                } catch (Exception e) {
                    String defaultErrorMessageForInstanceDeletion = getDefaultErrorMessageForInstanceDeletion(instanceId, deleteOperation);
                    if (deleteOperation != null && deleteOperation.getError() != null) {
                        LOGGER.warn("Waiting for GCP instance delete was resulting in error: {}", deleteOperation.getError().getErrors());
                    }
                    LOGGER.error(defaultErrorMessageForInstanceDeletion, e);
                    throw new TestFailException(defaultErrorMessageForInstanceDeletion, e);
                }
                if ("DONE".equals(deleteOperation.getStatus())) {
                    Log.log(LOGGER, format(" Gcp Instance: %s state is DELETED ", instanceId));
                } else {
                    String defaultErrorMessageForInstanceDeletion = getDefaultErrorMessageForInstanceDeletion(instanceId, deleteOperation);
                    LOGGER.error(defaultErrorMessageForInstanceDeletion);
                    throw new TestFailException(defaultErrorMessageForInstanceDeletion);
                }
            } catch (GoogleJsonResponseException e) {
                if (!e.getMessage().contains("Not Found")) {
                    handleGeneralInstanceDeletionError(instance.getKey(), e);
                } else {
                    LOGGER.info(format("Gcp instance [%s] is not found, thus it is deleted.", instanceId));
                }
            } catch (IOException e) {
                handleGeneralInstanceDeletionError(instanceId, e);
            }
        }
    }

    private void handleGeneralInstanceDeletionError(String instanceId, IOException e) {
        String errorMessage = format("Failed to invoke GCP instance deletion for instance [%s]: %s", instanceId, e.getMessage());
        LOGGER.error(errorMessage);
        throw new TestFailException(errorMessage, e);
    }

    private String getDefaultErrorMessageForInstanceDeletion(String instanceId, Operation deleteOperation) {
        if (deleteOperation != null) {
            return format(" Gcp Instance [%s] deletion was not successful, actual state of deletion is: %s, status message: %s",
                    instanceId, deleteOperation.getStatus(), deleteOperation.getStatusMessage());
        } else {
            return format(" Gcp Instance [%s] deletion was not successful, delete operation is null.", instanceId);
        }
    }

    private String getDefaultErrorMessageForInstanceStop(String instanceId, Operation stopOperation) {
        if (stopOperation != null) {
            return format(" Gcp Instance [%s] stop was not successful, actual state of stop is: %s, status message: %s",
                    instanceId, stopOperation.getStatus(), stopOperation.getStatusMessage());
        } else {
            return format(" Gcp Instance [%s] stop was not successful, stop operation is null.", instanceId);
        }
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        LOGGER.info("Stopping instances: '{}'", String.join(", ", instanceIds));
        Compute compute = buildCompute();
        Operation stopInstanceOperation = new Operation();
        for (String instanceId : instanceIds) {
            try {
                stopInstanceOperation = compute
                        .instances()
                        .stop(getProjectId(), getAvailabilityZone(), instanceId)
                        .execute();
                Log.log(LOGGER, format(" Gcp instance [%s] state is [%s] with message: %s", instanceId, stopInstanceOperation.getStatus(),
                        stopInstanceOperation.getStatusMessage()));
                stopInstanceOperation = waitForComplete(compute, stopInstanceOperation, getProjectId(), TIMEOUT);
            } catch (Exception e) {
                String defaultErrorMessageForInstanceStop = getDefaultErrorMessageForInstanceStop(instanceId, stopInstanceOperation);
                if (stopInstanceOperation != null && stopInstanceOperation.getError() != null) {
                    LOGGER.error("Waiting for GCP instance stop was resulting in error: {}", stopInstanceOperation.getError().getErrors());
                }
                LOGGER.error(format("Failed to wait for the STOPPED state on GCP instance: '%s'", instanceId), e);
                throw new TestFailException(defaultErrorMessageForInstanceStop);
            }
            if ("DONE".equals(stopInstanceOperation.getStatus())) {
                Log.log(LOGGER, format(" Gcp Instance: %s state is STOPPED ", instanceId));
            } else {
                String defaultErrorMessageForInstanceStop = getDefaultErrorMessageForInstanceStop(instanceId, stopInstanceOperation);
                LOGGER.error(defaultErrorMessageForInstanceStop);
                throw new TestFailException(defaultErrorMessageForInstanceStop);
            }
        }
    }

    public static Operation waitForComplete(Compute compute, Operation operation, String projectId, long timeout) throws Exception {
        long elapsed = 0;
        long start = System.currentTimeMillis();
        int attemps = 0;
        String zone = operation.getZone();
        if (zone != null) {
            String[] bits = zone.split("/");
            zone = bits[bits.length - 1];
        }
        String status = operation.getStatus();
        String statusMessage = operation.getStatusMessage();
        String opId = operation.getName();
        String operationDetails;
        while (operation != null && !"DONE".equals(status)) {
            Thread.sleep(POLLING_INTERVAL);
            elapsed = System.currentTimeMillis() - start;
            operationDetails = operation.getDescription();
            statusMessage = operation.getStatusMessage();
            if (elapsed >= timeout) {
                LOGGER.error("Waiting for operation: [{}] timed out! | Actual details {} | Actual status {}::{}",
                        opId, operationDetails, status, statusMessage);
                throw new TestFailException(format("Waiting for operation: [%s] timed out! | Actual details %s | Actual status %s::%s",
                        opId, operationDetails, status, statusMessage));
            }
            LOGGER.info("Waiting for operation: [{}] have been done | Actual status {}::{} | Elapsed rounds {} and time {} ms.",
                    opId, status, statusMessage, attemps, elapsed);
            if (zone != null) {
                Get get = compute.zoneOperations().get(projectId, zone, opId);
                operation = get.execute();
            } else {
                GlobalOperations.Get get = compute.globalOperations().get(projectId, opId);
                operation = get.execute();
            }
            if (operation != null) {
                status = operation.getStatus();
            }
            attemps++;
        }
        LOGGER.info("Waiting for operation: [{}] have been done | Actual status {}::{} | Elapsed rounds {} and time {} ms.",
                opId, status, statusMessage, attemps, elapsed);
        return operation;
    }

    public URI getBaseLocationUri() {
        try {
            return new URI(getBaseLocation());
        } catch (Exception e) {
            LOGGER.error("Google GCS base location path: '{}' is not a valid URI!", getBaseLocation());
            throw new TestFailException(format(" Google GCS base location path: '%s' is not a valid URI! ", getBaseLocation()));
        }
    }

    public URI getBaseLocationUri(String baseLocation) {
        try {
            return new URI(baseLocation);
        } catch (Exception e) {
            LOGGER.error("Google GCS base location path: '{}' is not a valid URI!", baseLocation);
            throw new TestFailException(format(" Google GCS base location path: '%s' is not a valid URI! ", baseLocation));
        }
    }

    public String getBucketName(String baseLocation) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        return baseLocationUri.getHost();
    }

    public String getKeyPrefix(String baseLocation) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        return StringUtils.removeStart(baseLocationUri.getPath(), "/");
    }

    public void listBucketSelectedObject(String baseLocation, String selectedObject, boolean zeroContent) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        String bucketName = getBucketName(baseLocation);
        String keyPrefix = getKeyPrefix(baseLocation);
        Page<Blob> blobs;
        List<Blob> filteredBlobs;

        Log.log(LOGGER, format(" Google GCS URI: %s", baseLocationUri));
        Log.log(LOGGER, format(" Google GCS Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Google GCS Key Prefix: %s", keyPrefix));
        Log.log(LOGGER, format(" Google GCS Object: %s", selectedObject));

        try (Storage storage = buildStorage()) {
            blobs = storage
                    .list(bucketName, BlobListOption.prefix(keyPrefix + '/'));
        } catch (Exception e) {
            Log.error(LOGGER, format("GCP GCS bucket '%s' is not present or accessible at base location '%s'", bucketName, baseLocationUri), e);
            throw new TestFailException(format("GCP GCS bucket '%s' is not present or accessible at base location '%s'", bucketName, baseLocationUri), e);
        }

        if (blobs == null || blobs.getValues() == null) {
            Log.error(LOGGER, "Google GCS path: '{}' does not exist!", keyPrefix);
            throw new TestFailException(format(" Google GCS path: '%s' does not exist! ", keyPrefix));
        } else {
            Iterable<Blob> blobListing = blobs.iterateAll();
            filteredBlobs = StreamSupport
                    .stream(blobListing.spliterator(), false)
                    .filter(listedBlob -> StringUtils.remove(listedBlob.getName(), "/").contains(StringUtils.remove(selectedObject, "/")))
                    .collect(Collectors.toList());

            if (filteredBlobs.isEmpty()) {
                Log.error(LOGGER, "Google GCS object: %s has 0 sub-objects or it is not present!", selectedObject);
                throw new TestFailException(format("Google GCS object: %s has 0 sub-objects or it is not present!", selectedObject));
            } else {
                Log.log(LOGGER, format(" Google GCS object: '%s' contains '%d' sub-objects or present with occurences.", selectedObject, filteredBlobs.size()));
                for (Blob filteredBlob : filteredBlobs.stream().limit(10).collect(Collectors.toList())) {
                    if (filteredBlob.getSize().compareTo(0L) == 0 && !zeroContent) {
                        /**
                         * Directory blobs:
                         * - BlobId.getGeneration() returns null,
                         * - BlobInfo.getSize() returns 0
                         * - BlobInfo.isDirectory() returns true
                         */
                        if (filteredBlob.isDirectory()) {
                            LOGGER.warn("Google GCS path: '{}' has 0 bytes of content!", filteredBlob.getName());
                        } else {
                            LOGGER.error("Google GCS path: '{}' has 0 bytes of content!", filteredBlob.getName());
                            throw new TestFailException(format("Google GCS path: '%s' has 0 bytes of content!", filteredBlob.getName()));
                        }
                    }
                }
            }
        }
    }

    public void deleteNonVersionedBucket(String baseLocation) {
        URI baseLocationUri = getBaseLocationUri(baseLocation);
        String bucketName = getBucketName(baseLocation);
        String keyPrefix = getKeyPrefix(baseLocation);
        Blob blob;
        Storage builtStorage;

        Log.log(LOGGER, format(" Google GCS URI: %s", baseLocationUri));
        Log.log(LOGGER, format(" Google GCS Bucket: %s", bucketName));
        Log.log(LOGGER, format(" Google GCS Key Prefix: %s", keyPrefix));

        try (Storage storage = buildStorage()) {
            blob = storage.get(bucketName, keyPrefix);
            builtStorage = storage;
        } catch (Exception e) {
            Log.error(LOGGER, format("GCP GCS bucket '%s' is not present or accessible at base location '%s'", bucketName, baseLocationUri), e);
            throw new TestFailException(format("GCP GCS bucket '%s' is not present or accessible at base location '%s'", bucketName, baseLocationUri), e);
        }
        if (blob.exists()) {
            try {
                BlobSourceOption precondition =
                        BlobSourceOption.generationMatch(blob.getGeneration());
                builtStorage.delete(bucketName, keyPrefix, precondition);
            } catch (StorageException e) {
                Log.error(LOGGER, format("Failed to delete bucket from base location '%s'", baseLocation));
                throw new TestFailException(format("Failed to delete bucket from base location '%s'", baseLocation), e);
            }
        } else {
            Log.log(LOGGER, "Google GCS path: '{}' does not exist! It has already been deleted.", keyPrefix);
        }
    }

    public String getLoggingUrl(String baseLocation, String clusterLogPath) {
        if (StringUtils.isNotBlank((baseLocation))) {
            URI baseLocationUri = getBaseLocationUri(baseLocation);
            String bucketName = getBucketName(baseLocation);
            String logPath = baseLocationUri.getPath();

            Log.log(LOGGER, format(" Google GCS URI: %s", baseLocationUri));
            Log.log(LOGGER, format(" Google GCS Bucket: %s", bucketName));
            Log.log(LOGGER, format(" Google GCS Log Path: %s", logPath));
            Log.log(LOGGER, format(" Google GCS Cluster Logs: %s", clusterLogPath));

            if (StringUtils.contains(getKeyPrefix(baseLocation), clusterLogPath)) {
                return format("https://console.cloud.google.com/storage/browser/%s/%s?project=gcp-dev-cloudbreak",
                        bucketName, getKeyPrefix(baseLocation));
            } else {
                return format("https://console.cloud.google.com/storage/browser/%s/%s%s?project=gcp-dev-cloudbreak",
                        bucketName, getKeyPrefix(baseLocation), clusterLogPath);
            }
        } else {
            return null;
        }
    }

    public Map<String, String> listAvailabilityZonesForVms(Map<String, String> instanceZoneMap) {
        Map<String, String> availabilityZoneMap = new HashMap<>();
        Compute compute = buildCompute();
        for (Map.Entry<String, String> instanceInfo : instanceZoneMap.entrySet()) {
            Instance instance = null;
            try {
                instance = compute.instances().get(getProjectId(), instanceInfo.getValue(), instanceInfo.getKey()).execute();
            } catch (IOException e) {

            }
            if (instance != null) {
                availabilityZoneMap.put(instance.getName(), instance.getZone());
            }
        }
        return availabilityZoneMap;
    }
}
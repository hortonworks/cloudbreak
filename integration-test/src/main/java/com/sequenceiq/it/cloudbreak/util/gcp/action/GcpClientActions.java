package com.sequenceiq.it.cloudbreak.util.gcp.action;

import static java.lang.String.format;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.gcp.client.GcpClient;

@Component
public class GcpClientActions extends GcpClient {
    protected static final int TIMEOUT = 600000;

    protected static final int POLLING_INTERVAL = 10000;

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpClientActions.class);

    @Inject
    private GcpProperties gcpProperties;

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        LOGGER.info("Collect tags/labels for instance ids: '{}'", String.join(", ", instanceIds));
        Map<String, Map<String, String>> instanceTags = new HashMap<>();
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Instance instance = compute
                        .instances()
                        .get(getProjectId(), gcpProperties.getAvailabilityZone(), instanceId)
                        .execute();
                instanceTags.put(instanceId, instance.getLabels());
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
        return instanceTags;
    }

    public List<String> listInstanceDiskNames(List<String> instanceIds) {
        LOGGER.info("Collect disk names for instance ids: '{}'", String.join(", ", instanceIds));
        Map<String, List<String>> instanceIdDiskNamesMap = new HashMap<>();
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Instance instance = compute
                        .instances()
                        .get(getProjectId(), gcpProperties.getAvailabilityZone(), instanceId)
                        .execute();
                List<String> attachedDiskNames = instance
                        .getDisks()
                        .stream()
                        .filter(ad -> !ad.getBoot())
                        .map(AttachedDisk::getDeviceName)
                        .collect(Collectors.toList());
                instanceIdDiskNamesMap.put(instanceId, attachedDiskNames);
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
        instanceIdDiskNamesMap.forEach((instanceId, diskNames) -> Log.log(LOGGER, format(" Attached disk names are %s for [%s] instance ",
                diskNames.toString(), instanceId)));
        return instanceIdDiskNamesMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        LOGGER.info("Deleting instances: '{}'", String.join(", ", instanceIds));
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Operation stopInstanceResponse = compute
                        .instances()
                        .delete(getProjectId(), gcpProperties.getAvailabilityZone(), instanceId)
                        .execute();
                Log.log(LOGGER, format(" Gcp instance [%s] state is [%s] with message: %s", instanceId, stopInstanceResponse.getStatus(),
                        stopInstanceResponse.getStatusMessage()));
                waitForComplete(compute, stopInstanceResponse, getProjectId(), TIMEOUT);
                if (stopInstanceResponse.getStatus().equals("DONE")) {
                    Log.log(LOGGER, format(" Gcp Instance: %s state is DELETED ", instanceId));
                } else {
                    LOGGER.error("Gcp Instance: {} delete has not been successful. So the actual state is: {} with message: {}",
                            instanceId, stopInstanceResponse.getStatus(), stopInstanceResponse.getStatusMessage());
                    throw new TestFailException(" Gcp Instance: " + instanceId
                            + " delete has not been successful, because of the actual state is: "
                            + stopInstanceResponse.getStatus()
                            + " with message: "
                            + stopInstanceResponse.getStatusMessage());
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        LOGGER.info("Stopping instances: '{}'", String.join(", ", instanceIds));
        Compute compute = buildCompute();
        for (String instanceId : instanceIds) {
            try {
                Operation stopInstanceResponse = compute
                        .instances()
                        .stop(getProjectId(), gcpProperties.getAvailabilityZone(), instanceId)
                        .execute();
                Log.log(LOGGER, format(" Gcp instance [%s] state is [%s] with message: %s", instanceId, stopInstanceResponse.getStatus(),
                        stopInstanceResponse.getStatusMessage()));
                waitForComplete(compute, stopInstanceResponse, getProjectId(), TIMEOUT);
                if (stopInstanceResponse.getStatus().equals("DONE")) {
                    Log.log(LOGGER, format(" Gcp Instance: %s state is STOPPED ", instanceId));
                } else {
                    LOGGER.error("Gcp Instance: {} stop has not been successful. So the actual state is: {} with message: {}",
                            instanceId, stopInstanceResponse.getStatus(), stopInstanceResponse.getStatusMessage());
                    throw new TestFailException(" Gcp Instance: " + instanceId
                            + " stop has not been successful, because of the actual state is: "
                            + stopInstanceResponse.getStatus()
                            + " with message: "
                            + stopInstanceResponse.getStatusMessage());
                }
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to get the details of the instance from Gcp with instance id: '%s'", instanceId), e);
            }
        }
    }

    public static Operation.Error waitForComplete(Compute compute, Operation operation, String projectId, long timeout) throws Exception {
        String operationDetails = "";
        long elapsed = 0;
        long start = System.currentTimeMillis();
        int attemps = 0;
        String zone = operation.getZone();
        if (zone != null) {
            String[] bits = zone.split("/");
            zone = bits[bits.length - 1];
        }
        String status = operation.getStatus();
        String opId = operation.getName();
        while (operation != null && !"DONE".equals(status)) {
            Thread.sleep(POLLING_INTERVAL);
            elapsed = System.currentTimeMillis() - start;
            operationDetails = operation.getDescription();
            if (elapsed >= timeout) {
                LOGGER.error("Timed out waiting for operation: [{}] with details: {} to complete!", opId, operationDetails);
                throw new TestFailException("Timed out waiting for operation: [" + opId + "] with details: " + operationDetails + " to complete!");
            }
            LOGGER.info("Waiting for operation: [{}] with details: {} - elapsed rounds [{}] and time [{}] ms", opId, operationDetails, attemps, elapsed);
            if (zone != null) {
                Compute.ZoneOperations.Get get = compute.zoneOperations().get(projectId, zone, opId);
                operation = get.execute();
            } else {
                Compute.GlobalOperations.Get get = compute.globalOperations().get(projectId, opId);
                operation = get.execute();
            }
            if (operation != null) {
                status = operation.getStatus();
            }
            attemps++;
        }
        LOGGER.info("Waiting for operation: [{}] with details: {} have been done. Elapsed rounds [{}] and time [{}] ms.", opId, operationDetails, attemps,
                elapsed);
        return operation == null ? null : operation.getError();
    }

    public void listBucketSelectedObject(String baseLocation, String selectedObject, boolean zeroContent) {
        LOGGER.info("List bucket from base location: '{}', with selected object: '{}', without content: '{}'", baseLocation, selectedObject, zeroContent);
        Storage storage = buildStorage();
        try {
            Storage.Objects.Get operation = storage.objects().get(baseLocation, selectedObject);
            StorageObject storageObject = operation.execute();
            if (storageObject.getSize().compareTo(BigInteger.ZERO) == 0 && !zeroContent) {
                String objectPath = StringUtils.join(List.of(baseLocation, selectedObject), "/");
                LOGGER.error("Google GCS path: {} has 0 bytes of content!", objectPath);
                throw new TestFailException(String.format(" Google GCS path: %s has 0 bytes of content! ", objectPath));
            }
        } catch (IOException ioException) {
            String msg = String.format("Failed to list bucket object '%s' from base location '%s'", baseLocation, selectedObject);
            LOGGER.error(msg, ioException);
            throw new TestFailException(msg, ioException);
        }
    }

    public void deleteNonVersionedBucket(String baseLocation) {
        LOGGER.info("Delete bucket from base location: '{}'", baseLocation);
        Storage storage = buildStorage();
        try {
            Storage.Buckets.Delete operation = storage.buckets().delete(baseLocation);
            operation.execute();
        } catch (IOException ioException) {
            String msg = String.format("Failed to delete bucket from base location '%s'", baseLocation);
            LOGGER.error(msg, ioException);
            throw new TestFailException(msg, ioException);
        }
    }
}
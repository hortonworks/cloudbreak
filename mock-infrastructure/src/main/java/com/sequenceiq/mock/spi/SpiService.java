package com.sequenceiq.mock.spi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class SpiService {

    @Inject
    private SpiStoreService spiStoreService;

    public void terminateInstance(String mockUuid, String instanceId) {
        SpiDto read = spiStoreService.read(mockUuid);
        Optional<CloudVmMetaDataStatus> cloudVmMetaDataStatus = read.getVmMetaDataStatuses().stream()
                .filter(md -> md.getCloudVmInstanceStatus().getCloudInstance().getInstanceId().equals(instanceId))
                .findFirst();
        cloudVmMetaDataStatus.ifPresent(vmMetaDataStatus -> spiStoreService.remove(mockUuid, vmMetaDataStatus));
    }

    public CloudVmInstanceStatus startInstance(String mockUuid, String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = getByInstanceId(mockUuid, instanceId);
        CloudVmMetaDataStatus newMD = spiStoreService.modifyInstanceStatus(mockUuid, vmMetaDataStatus, InstanceStatus.STARTED);
        return newMD.getCloudVmInstanceStatus();
    }

    public List<CloudVmMetaDataStatus> startInstances(String mockUuid, List<CloudInstance> cloudInstances) {
        SpiDto read = spiStoreService.read(mockUuid);
        List<CloudVmMetaDataStatus> vmToStart = collectVmMetaDataStatuses(cloudInstances, read);
        return vmToStart.stream().map(md -> spiStoreService.modifyInstanceStatus(mockUuid, md, InstanceStatus.STARTED)).collect(Collectors.toList());
    }

    public List<CloudVmMetaDataStatus> collectVmMetaDataStatuses(List<CloudInstance> cloudInstances, String mockUuid) {
        SpiDto spiDto = spiStoreService.read(mockUuid);
        return collectVmMetaDataStatuses(cloudInstances, spiDto);
    }

    private List<CloudVmMetaDataStatus> collectVmMetaDataStatuses(List<CloudInstance> cloudInstances, SpiDto read) {
        List<CloudVmMetaDataStatus> vmToStart;
        if (CollectionUtils.isEmpty(cloudInstances)) {
            vmToStart = read.getVmMetaDataStatuses();
        } else {
            vmToStart = read.getVmMetaDataStatuses().stream()
                    .filter(md -> cloudInstances.stream()
                            .anyMatch(c -> md.getCloudVmInstanceStatus().getCloudInstance().getInstanceId().equals(c.getInstanceId())))
                    .collect(Collectors.toList());
        }
        return vmToStart;
    }

    public CloudVmInstanceStatus stopInstance(String mockUuid, String instanceId) {
        CloudVmMetaDataStatus vmMetaDataStatus = getByInstanceId(mockUuid, instanceId);
        CloudVmMetaDataStatus newMD = spiStoreService.modifyInstanceStatus(mockUuid, vmMetaDataStatus, InstanceStatus.STOPPED);
        return newMD.getCloudVmInstanceStatus();
    }

    public List<CloudVmMetaDataStatus> stopInstances(String mockUuid, List<CloudInstance> cloudInstances) {
        SpiDto read = spiStoreService.read(mockUuid);
        List<CloudVmMetaDataStatus> vmToStop = collectVmMetaDataStatuses(cloudInstances, read);
        return vmToStop.stream().map(md -> spiStoreService.modifyInstanceStatus(mockUuid, md, InstanceStatus.STOPPED)).collect(Collectors.toList());
    }

    public CloudVmMetaDataStatus getByInstanceId(String mockUuid, String instanceId) {
        List<CloudVmMetaDataStatus> metadata = spiStoreService.getMetadata(mockUuid);
        Optional<CloudVmMetaDataStatus> first = metadata.stream()
                .filter(md -> md.getCloudVmInstanceStatus().getCloudInstance().getInstanceId().equals(instanceId))
                .findFirst();
        if (first.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot find CloudVmMetaDataStatus by instanceId: " + instanceId);
        }
        return first.get();
    }
}

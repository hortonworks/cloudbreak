package com.sequenceiq.mock.spi.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.freeipa.FreeipaStoreService;
import com.sequenceiq.mock.salt.SaltStoreService;
import com.sequenceiq.mock.service.ResponseModifierService;
import com.sequenceiq.mock.spi.SpiService;
import com.sequenceiq.mock.spi.SpiStoreService;

@RestController
@RequestMapping("/{mock_uuid}/spi")
public class SpiController {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private SpiService spiService;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private FreeipaStoreService freeipaStoreService;

    @Inject
    private SaltStoreService saltStoreService;

    @Inject
    private ResponseModifierService responseModifierService;

    @PostMapping("/launch")
    public List<CloudVmInstanceStatus> launch(@PathVariable("mock_uuid") String mockuuid, @RequestBody CloudStack cloudStack) {
        List<CloudVmInstanceStatus> vms = spiStoreService.store(mockuuid, cloudStack)
                .stream()
                .map(CloudVmMetaDataStatus::getCloudVmInstanceStatus)
                .collect(Collectors.toList());
        saltStoreService.create(mockuuid);
        return vms;
    }

    @PostMapping("/disable_add_instance")
    public void disableAddInstance(@PathVariable("mock_uuid") String mockuuid) {
        spiStoreService.disableAddInstance(mockuuid);
    }

    @PostMapping("/enable_add_instance")
    public void enableAddInstance(@PathVariable("mock_uuid") String mockuuid) {
        spiStoreService.enableAddInstance(mockuuid);
    }

    @DeleteMapping("/terminate")
    public void terminate(@PathVariable("mock_uuid") String mockuuid) {
        spiStoreService.terminate(mockuuid);
        clouderaManagerStoreService.terminate(mockuuid);
        freeipaStoreService.terminate(mockuuid);
        saltStoreService.terminate(mockuuid);
        responseModifierService.cleanByMockUuid(mockuuid);
    }

    @PostMapping("/add_instance")
    public List<CloudVmInstanceStatus> addInstance(@PathVariable("mock_uuid") String mockuuid, @RequestBody List<Group> groups) {
        return spiStoreService.resize(mockuuid, groups).stream().map(CloudVmMetaDataStatus::getCloudVmInstanceStatus).collect(Collectors.toList());
    }

    @PostMapping("/terminate_instances")
    public void terminateInstances(@PathVariable("mock_uuid") String mockuuid, @RequestBody List<CloudInstance> cloudInstances) {
        cloudInstances.forEach(cloudInstance -> spiService.terminateInstance(mockuuid, cloudInstance.getInstanceId()));
    }

    @GetMapping("/{instanceId}/start")
    public CloudVmInstanceStatus startInstance(@PathVariable("mock_uuid") String mockuuid, @PathVariable String instanceId) {
        return spiService.startInstance(mockuuid, instanceId);
    }

    @PostMapping("/start_instances")
    public List<CloudVmInstanceStatus> startInstances(@PathVariable("mock_uuid") String mockuuid, @RequestBody List<CloudInstance> cloudInstances) {
        return spiService.startInstances(mockuuid, cloudInstances).stream().map(CloudVmMetaDataStatus::getCloudVmInstanceStatus).collect(Collectors.toList());
    }

    @GetMapping("/{instanceId}/stop")
    public CloudVmInstanceStatus stopInstance(@PathVariable("mock_uuid") String mockuuid, @PathVariable String instanceId) {
        return spiService.stopInstance(mockuuid, instanceId);
    }

    @GetMapping("/{instanceId}/terminate")
    public void terminateInstance(@PathVariable("mock_uuid") String mockuuid, @PathVariable String instanceId) {
        spiService.terminateInstance(mockuuid, instanceId);
    }

    @PostMapping("/stop_instances")
    public List<CloudVmInstanceStatus> stopInstances(@PathVariable("mock_uuid") String mockuuid, @RequestBody List<CloudInstance> cloudInstances) {
        return spiService.stopInstances(mockuuid, cloudInstances).stream().map(CloudVmMetaDataStatus::getCloudVmInstanceStatus).collect(Collectors.toList());
    }

    @PostMapping("/reboot_instances")
    public void rebootInstances(@PathVariable("mock_uuid") String mockuuid) {
        throw new UnsupportedOperationException("Cannot reboot");
    }

    @GetMapping("/{instanceId}/reboot")
    public CloudVmInstanceStatus rebootInstance(@PathVariable("mock_uuid") String mockuuid, @PathVariable String instanceId) {
        throw new UnsupportedOperationException("Cannot reboot");
    }

    @PostMapping("/cloud_instance_statuses")
    public List<CloudVmInstanceStatus> cloudInstanceStatuses(@PathVariable("mock_uuid") String mockuuid, @RequestBody List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : spiService.collectVmMetaDataStatuses(cloudInstances, mockuuid)) {
            cloudVmInstanceStatuses.add(cloudVmMetaDataStatus.getCloudVmInstanceStatus());
        }
        return cloudVmInstanceStatuses;
    }

    @PostMapping("/cloud_metadata_statuses")
    public List<CloudVmMetaDataStatus> cloudMetadataStatuses(@PathVariable("mock_uuid") String mockuuid, @RequestBody List<CloudInstance> cloudInstances) {
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();

        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : spiStoreService.getMetadata(mockuuid)) {
            InstanceTemplate oldTemplate = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getTemplate();
            Optional<CloudInstance> cloudInstance = cloudInstances.stream()
                    .filter(instance -> Objects.equals(instance.getTemplate().getPrivateId(), oldTemplate.getPrivateId())).findFirst();
            if (cloudInstance.isPresent()) {
                cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
            }
        }
        return cloudVmMetaDataStatuses;
    }
}

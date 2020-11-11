package com.sequenceiq.mock.legacy.spi.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.response.MockResponse;
import com.sequenceiq.mock.legacy.service.ResponseModifierService;

@RestController
@RequestMapping("/spi")
public class SpiLegacyController {

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private ResponseModifierService responseModifierService;

    @GetMapping("/")
    public void launch() {
    }

    @PostMapping("/terminate_instances")
    public void terminateInstances(@RequestBody List<CloudInstance> cloudInstances) {
        cloudInstances.forEach(cloudInstance -> defaultModelService.terminateInstance(cloudInstance.getInstanceId()));
    }

    @GetMapping("/{instanceId}/start")
    public CloudVmInstanceStatus startInstance(@PathVariable String instanceId) {
        return defaultModelService.startInstance(instanceId);
    }

    @PostMapping("/start_instances")
    public void startInstances() {
        defaultModelService.startAllInstances();
    }

    @GetMapping("/{instanceId}/stop")
    public CloudVmInstanceStatus stopInstance(@PathVariable String instanceId) {
        return defaultModelService.stopInstance(instanceId);
    }

    @GetMapping("/{instanceId}/terminate")
    public void terminateInstance(@PathVariable String instanceId) {
        defaultModelService.terminateInstance(instanceId);
    }

    @PostMapping("/stop_instances")
    public void stopInstances() {
        defaultModelService.stopAllInstances();
    }

    @PostMapping("/reboot_instances")
    public void rebootInstance() {

    }

    @GetMapping("/{instanceId}/reboot")
    public CloudVmInstanceStatus rebootInstance(@PathVariable String instanceId) {
        CloudInstance instance = new CloudInstance(instanceId, null, null);
        return new CloudVmInstanceStatus(instance, InstanceStatus.STARTED);
    }

    @PostMapping("/cloud_instance_statuses")
    public List<CloudVmInstanceStatus> cloudInstanceStatuses() {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : defaultModelService.getInstanceMap().entrySet()) {
            cloudVmInstanceStatuses.add(stringCloudVmMetaDataStatusEntry.getValue().getCloudVmInstanceStatus());
        }
        return cloudVmInstanceStatuses;
    }

    @PostMapping("/cloud_metadata_statuses")
    public List<CloudVmMetaDataStatus> cloudMetadataStatuses(@RequestBody List<CloudInstance> cloudInstances) {
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();
        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : defaultModelService.getInstanceMap().entrySet()) {
            CloudVmMetaDataStatus oldCloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            InstanceTemplate oldTemplate = oldCloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getTemplate();
            Optional<CloudInstance> cloudInstance = cloudInstances.stream()
                    .filter(instance -> Objects.equals(instance.getTemplate().getPrivateId(), oldTemplate.getPrivateId())).findFirst();
            if (cloudInstance.isPresent()) {
                CloudInstance newCloudInstance = new CloudInstance(stringCloudVmMetaDataStatusEntry.getKey(),
                        cloudInstance.get().getTemplate(),
                        cloudInstance.get().getAuthentication(),
                        cloudInstance.get().getParameters());
                CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(newCloudInstance,
                        oldCloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus());
                CloudVmMetaDataStatus newCloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, oldCloudVmMetaDataStatus.getMetaData());
                cloudVmMetaDataStatuses.add(newCloudVmMetaDataStatus);
            }
        }
        return cloudVmMetaDataStatuses;
    }

    @PostMapping("/register_public_key")
    public void registerPublicKey() {
    }

    @PostMapping("/unregister_public_key")
    public void unregisterPublicKey(@RequestBody String body) {
    }

    @GetMapping("/get_public_key/{publicKeyId}")
    public Boolean getPubicKeyId(@PathVariable("publicKeyId") String publicKeyId) {
        MockResponse response = responseModifierService.getResponse("get", "/spi/get_public_key/{publicKeyId}");
        if (response == null) {
            return true;
        }
        return (Boolean) response.getResponse();
    }

}

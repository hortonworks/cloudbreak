package com.sequenceiq.mock.spi;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.mock.clouderamanager.DefaultModelService;

@Service
public class SpiStoreService {

    @Inject
    private DefaultModelService defaultModelService;

    private final Map<String, SpiDto> spiDtoMap = new HashMap<>();

    public List<CloudVmMetaDataStatus> store(String mockuuid, CloudStack cloudStack) {
        List<CloudVmMetaDataStatus> instances = defaultModelService.createInstances(mockuuid, cloudStack.getGroups());
        SpiDto spiDto = new SpiDto(mockuuid, cloudStack);
        spiDto.setVmMetaDataStatuses(instances);
        spiDtoMap.put(mockuuid, spiDto);
        return instances;
    }

    public SpiDto read(String mockuuid) {
        SpiDto spiDto = spiDtoMap.get(mockuuid);
        if (spiDto == null) {
            throw new ResponseStatusException(NOT_FOUND, "SpiDto cannot be found by uuid: " + mockuuid);
        }
        return spiDto;
    }

    public List<CloudVmMetaDataStatus> getMetadata(String mockuuid) {
        SpiDto spiDto = read(mockuuid);
        return spiDto.getVmMetaDataStatuses();
    }

    public void remove(String mockUuid, CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        read(mockUuid).getVmMetaDataStatuses().remove(cloudVmMetaDataStatus);
    }

    public CloudVmMetaDataStatus modifyInstanceStatus(String mockUuid, CloudVmMetaDataStatus metaDataStatus, InstanceStatus instanceStatus) {
        SpiDto spiDto = read(mockUuid);
        CloudVmInstanceStatus currentInstance = metaDataStatus.getCloudVmInstanceStatus();
        CloudVmInstanceStatus newInstanceStatus = new CloudVmInstanceStatus(currentInstance.getCloudInstance(), instanceStatus);
        CloudVmMetaDataStatus newVmMetaData = new CloudVmMetaDataStatus(newInstanceStatus, metaDataStatus.getMetaData());
        spiDto.getVmMetaDataStatuses().remove(metaDataStatus);
        spiDto.getVmMetaDataStatuses().add(newVmMetaData);
        return newVmMetaData;
    }

    public List<CloudVmMetaDataStatus> resize(String mockuuid, List<Group> groups) {
        List<CloudVmMetaDataStatus> instances = defaultModelService.createInstances(mockuuid, groups);
        read(mockuuid).getVmMetaDataStatuses().addAll(instances);
        return instances;
    }

    public void terminate(String mockuuid) {
        spiDtoMap.remove(mockuuid);
    }

    public Collection<SpiDto> getAll() {
        return spiDtoMap.values();
    }
}

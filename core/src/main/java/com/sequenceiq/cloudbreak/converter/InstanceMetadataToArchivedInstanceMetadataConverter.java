package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.instance.ArchivedInstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetadataToArchivedInstanceMetadataConverter {

    public ArchivedInstanceMetaData convert(InstanceMetaData instanceMetaData) {
        ArchivedInstanceMetaData result = new ArchivedInstanceMetaData();

        result.setInstanceGroup(instanceMetaData.getInstanceGroup());
        result.setPrivateIp(instanceMetaData.getPrivateIp());
        result.setPublicIp(instanceMetaData.getPublicIp());
        result.setSshPort(instanceMetaData.getSshPort());
        result.setId(instanceMetaData.getId());
        result.setPrivateId(instanceMetaData.getPrivateId());
        result.setInstanceId(instanceMetaData.getInstanceId());
        result.setAmbariServer(instanceMetaData.getAmbariServer());
        result.setClusterManagerServer(instanceMetaData.getClusterManagerServer());
        result.setDiscoveryFQDN(instanceMetaData.getDiscoveryFQDN());
        result.setInstanceStatus(instanceMetaData.getInstanceStatus());
        result.setStartDate(instanceMetaData.getStartDate());
        result.setTerminationDate(instanceMetaData.getTerminationDate());
        result.setLocalityIndicator(instanceMetaData.getLocalityIndicator());
        result.setInstanceMetadataType(instanceMetaData.getInstanceMetadataType());
        result.setServerCert(instanceMetaData.getServerCert());
        result.setSubnetId(instanceMetaData.getSubnetId());
        result.setAvailabilityZone(instanceMetaData.getAvailabilityZone());
        result.setRackId(instanceMetaData.getRackId());
        result.setInstanceName(instanceMetaData.getInstanceName());
        result.setImage(instanceMetaData.getImage());
        result.setStatusReason(instanceMetaData.getStatusReason());
        result.setLifeCycle(instanceMetaData.getLifeCycle());
        result.setVariant(instanceMetaData.getVariant());

        return result;
    }
}

package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class StackToAutoscaleStackV4ResponseConverter {

    public AutoscaleStackV4Response convert(Stack source) {
        AutoscaleStackV4Response result = new AutoscaleStackV4Response();
        result.setTenant(source.getWorkspace().getTenant().getName());
        result.setWorkspaceId(source.getWorkspace().getId());
        result.setUserId(source.getCreator().getUserId());
        result.setStackId(source.getId());
        result.setName(source.getName());
        result.setGatewayPort(source.getGatewayPort());
        result.setCreated(source.getCreated());
        result.setStatus(source.getStatus());
        result.setStackCrn(source.getResourceCrn());
        result.setTunnel(source.getTunnel());
        result.setCloudPlatform(source.getCloudPlatform());
        result.setUserCrn(source.getCreator().getUserCrn());
        result.setStackType(source.getType());
        result.setEnvironmentCrn(source.getEnvironmentCrn());

        if (source.getCluster() != null) {
            Cluster cluster = source.getCluster();
            result.setClusterManagerIp(cluster.getClusterManagerIp());
            result.setUserNamePath(cluster.getCloudbreakAmbariUserSecret());
            result.setPasswordPath(cluster.getCloudbreakAmbariPasswordSecret());
            result.setClusterStatus(source.getStatus());
        }
        return result;
    }
}

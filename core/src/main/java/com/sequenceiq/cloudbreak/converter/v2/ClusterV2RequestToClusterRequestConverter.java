package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;

@Component
public class ClusterV2RequestToClusterRequestConverter extends AbstractConversionServiceAwareConverter<ClusterV2Request, ClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV2RequestToClusterRequestConverter.class);

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Override
    public ClusterRequest convert(ClusterV2Request source) {
        ClusterRequest cluster = new ClusterRequest();
        cluster.setExecutorType(source.getExecutorType());
        cluster.setEmailNeeded(source.getEmailNeeded());
        cluster.setEmailTo(source.getEmailTo());
        cluster.setFileSystem(source.getFileSystem());
        cluster.setName(source.getName());
        if (source.getRdsConfigNames() != null && !source.getRdsConfigNames().isEmpty()) {
            cluster.setRdsConfigNames(source.getRdsConfigNames());
        }
        cluster.setProxyName(source.getProxyName());
        cluster.setLdapConfigName(source.getLdapConfigName());
        AmbariV2Request ambariRequest = source.getAmbari();
        if (ambariRequest != null) {
            cluster.setAmbariDatabaseDetails(ambariRequest.getAmbariDatabaseDetails());
            cluster.setAmbariRepoDetailsJson(ambariRequest.getAmbariRepoDetailsJson());
            cluster.setAmbariStackDetails(ambariRequest.getAmbariStackDetails());
            cluster.setBlueprintId(ambariRequest.getBlueprintId());
            cluster.setBlueprintName(ambariRequest.getBlueprintName());
            cluster.setConfigStrategy(ambariRequest.getConfigStrategy());
            cluster.setConnectedCluster(ambariRequest.getConnectedCluster());
            cluster.setEnableSecurity(ambariRequest.getEnableSecurity());
            cluster.setGateway(ambariRequest.getGateway());
            cluster.setKerberos(ambariRequest.getKerberos());
            cluster.setPassword(ambariRequest.getPassword());
            cluster.setUserName(ambariRequest.getUserName());
            cluster.setValidateBlueprint(ambariRequest.getValidateBlueprint());
            cluster.setAmbariSecurityMasterKey(ambariRequest.getAmbariSecurityMasterKey());
            if (sharedServiceConfigProvider.isConfigured(source)) {
                ConnectedClusterRequest connectedClusterRequest = new ConnectedClusterRequest();
                connectedClusterRequest.setSourceClusterName(source.getSharedService().getSharedCluster());
                cluster.setConnectedCluster(connectedClusterRequest);
            }
        }
        cluster.setHostGroups(new HashSet<>());
        return cluster;
    }

}

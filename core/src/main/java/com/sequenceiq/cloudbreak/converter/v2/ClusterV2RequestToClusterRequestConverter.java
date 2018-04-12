package com.sequenceiq.cloudbreak.converter.v2;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class ClusterV2RequestToClusterRequestConverter extends AbstractConversionServiceAwareConverter<ClusterV2Request, ClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV2RequestToClusterRequestConverter.class);

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
            cluster.setBlueprintCustomPropertiesAsString(ambariRequest.getBlueprintCustomProperties());
            cluster.setBlueprintId(ambariRequest.getBlueprintId());
            cluster.setBlueprintName(ambariRequest.getBlueprintName());
            cluster.setBlueprintInputs(ambariRequest.getBlueprintInputs());
            cluster.setConfigStrategy(ambariRequest.getConfigStrategy());
            cluster.setConnectedCluster(ambariRequest.getConnectedCluster());
            cluster.setEnableSecurity(ambariRequest.getEnableSecurity());
            cluster.setGateway(ambariRequest.getGateway());
            cluster.setKerberos(ambariRequest.getKerberos());
            cluster.setPassword(ambariRequest.getPassword());
            cluster.setUserName(ambariRequest.getUserName());
            cluster.setValidateBlueprint(ambariRequest.getValidateBlueprint());
            cluster.setAmbariSecurityMasterKey(ambariRequest.getAmbariSecurityMasterKey());
        }
        cluster.setHostGroups(new HashSet<>());
        return cluster;
    }
}

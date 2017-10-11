package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

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
        cluster.setLdapConfigId(source.getLdapConfigId());
        cluster.setName(source.getName());
        cluster.setRdsConfigIds(source.getRdsConfigIds());
        cluster.setRdsConfigJsons(source.getRdsConfigJsons());
        if (source.getAmbariRequest() != null) {
            cluster.setAmbariDatabaseDetails(source.getAmbariRequest().getAmbariDatabaseDetails());
            cluster.setAmbariRepoDetailsJson(source.getAmbariRequest().getAmbariRepoDetailsJson());
            cluster.setAmbariStackDetails(source.getAmbariRequest().getAmbariStackDetails());
            cluster.setBlueprintCustomPropertiesAsString(source.getAmbariRequest().getBlueprintCustomProperties());
            cluster.setBlueprintId(source.getAmbariRequest().getBlueprintId());
            cluster.setBlueprintName(source.getAmbariRequest().getBlueprintName());
            cluster.setBlueprintInputs(source.getAmbariRequest().getBlueprintInputs());
            cluster.setConfigStrategy(source.getAmbariRequest().getConfigStrategy());
            cluster.setConnectedCluster(source.getAmbariRequest().getConnectedCluster());
            cluster.setEnableSecurity(source.getAmbariRequest().getEnableSecurity());
            cluster.setGateway(source.getAmbariRequest().getGateway());
            cluster.setKerberos(source.getAmbariRequest().getKerberos());
            cluster.setPassword(source.getAmbariRequest().getPassword());
            cluster.setUserName(source.getAmbariRequest().getUserName());
            cluster.setValidateBlueprint(source.getAmbariRequest().getValidateBlueprint());
        }
        if (source.getByosRequest() != null) {
            cluster.setCustomContainer(source.getByosRequest().getCustomContainer());
            cluster.setCustomQueue(source.getByosRequest().getCustomQueue());
        }
        cluster.setHostGroups(new HashSet<>());
        return cluster;
    }
}

package com.sequenceiq.cloudbreak.converter.stack.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.VaultService;

@Component
public class ClusterToAutoscaleClusterResponseJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, AutoscaleClusterResponse> {

    @Inject
    private VaultService vaultService;

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        AutoscaleClusterResponse response = getConversionService().convert(source, AutoscaleClusterResponse.class);
        response.setPassword(vaultService.resolveSingleValue(source.getCloudbreakAmbariPassword()));
        response.setUserName(vaultService.resolveSingleValue(source.getCloudbreakAmbariUser()));
        return response;
    }
}

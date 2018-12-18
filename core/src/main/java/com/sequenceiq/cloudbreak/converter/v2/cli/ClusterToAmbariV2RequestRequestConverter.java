package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Component
public class ClusterToAmbariV2RequestRequestConverter extends AbstractConversionServiceAwareConverter<Cluster, AmbariV2Request> {

    @Override
    public AmbariV2Request convert(Cluster source) {
        AmbariV2Request ambariV2Request = new AmbariV2Request();
        ambariV2Request.setBlueprintName(source.getBlueprint().getName());
        prepareRepoDetails(source, ambariV2Request);
        ambariV2Request.setConfigStrategy(null);
        ambariV2Request.setConnectedCluster(null);
        ambariV2Request.setPassword("");
        ambariV2Request.setUserName("");
        ambariV2Request.setValidateBlueprint(null);
        if (source.getKerberosConfig() != null) {
            ambariV2Request.setKerberosConfigName(source.getKerberosConfig().getName());
        }
        if (source.getGateway() != null) {
            ambariV2Request.setGateway(getConversionService().convert(source.getGateway(), GatewayJson.class));
        }
        return ambariV2Request;
    }

    private void prepareRepoDetails(Cluster source, AmbariV2Request ambariV2Request) {
        ambariV2Request.setAmbariStackDetails(getComponent(source, ComponentType.HDP_REPO_DETAILS, StackRepoDetails.class, AmbariStackDetailsJson.class));
        ambariV2Request.setAmbariRepoDetailsJson(getComponent(source, ComponentType.AMBARI_REPO_DETAILS, AmbariRepo.class, AmbariRepoDetailsJson.class));
    }

    private <T> T getComponent(Cluster source, ComponentType hdpRepoDetails, Class<?> srcClss, Class<T> responseClss) {
        try {
            Optional<ClusterComponent> repoComponent = source.getComponents().stream().filter(it -> it.getComponentType() == hdpRepoDetails).findFirst();
            if (repoComponent.isPresent()) {
                return getConversionService().convert(repoComponent.get().getAttributes().get(srcClss), responseClss);
            }
            return null;
        } catch (IOException e) {
            throw new BadRequestException("Cannot deserialize the compnent: " + responseClss, e);
        }
    }
}

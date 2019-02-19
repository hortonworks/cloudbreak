package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Component
public class ClusterToAmbariV4RequestRequestConverter extends AbstractConversionServiceAwareConverter<Cluster, AmbariV4Request> {

    @Override
    public AmbariV4Request convert(Cluster source) {
        AmbariV4Request ambariRequest = new AmbariV4Request();
        ambariRequest.setClusterDefinitionName(source.getClusterDefinition().getName());
        prepareRepoDetails(source, ambariRequest);
        ambariRequest.setConfigStrategy(null);
        ambariRequest.setPassword("");
        ambariRequest.setUserName("");
        ambariRequest.setValidateClusterDefinition(null);
        return ambariRequest;
    }

    private void prepareRepoDetails(Cluster source, AmbariV4Request ambariV2Request) {
        ambariV2Request.setStackRepository(getComponent(source, ComponentType.HDP_REPO_DETAILS, StackRepoDetails.class, StackRepositoryV4Request.class));
        ambariV2Request.setRepository(getComponent(source, ComponentType.AMBARI_REPO_DETAILS, AmbariRepo.class, AmbariRepositoryV4Request.class));
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

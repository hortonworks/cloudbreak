package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.AmbariV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Component
public class ClusterToAmbariV4ResponseConverter extends AbstractConversionServiceAwareConverter<Cluster, AmbariV4Response> {

    @Override
    public AmbariV4Response convert(Cluster source) {
        AmbariV4Response response = new AmbariV4Response();
        response.setConfigStrategy(source.getConfigStrategy());
        response.setRepository(getComponent(source, ComponentType.AMBARI_REPO_DETAILS, AmbariRepo.class, AmbariRepositoryV4Response.class));
        response.setSecurityMasterKey(getConversionService().convert(source.getAmbariSecurityMasterKey(), SecretV4Response.class));
        response.setStackRepository(getComponent(source, ComponentType.HDP_REPO_DETAILS, StackRepoDetails.class, StackRepositoryV4Response.class));
        return response;
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

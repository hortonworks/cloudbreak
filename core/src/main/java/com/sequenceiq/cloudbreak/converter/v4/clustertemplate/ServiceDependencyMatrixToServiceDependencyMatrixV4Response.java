package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.ServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.ServiceDependencyMatrix;

@Component
public class ServiceDependencyMatrixToServiceDependencyMatrixV4Response {

    public ServiceDependencyMatrixV4Response convert(ServiceDependencyMatrix source) {
        ServiceDependencyMatrixV4Response serviceDependencyMatrixV4Response = new ServiceDependencyMatrixV4Response();

        Set<String> dependenciesV4Responses = new HashSet<>();
        dependenciesV4Responses.addAll(source.getDependencies().getServices());
        serviceDependencyMatrixV4Response.setDependencies(dependenciesV4Responses);

        Set<String> servicesV4Responses = new HashSet<>();
        servicesV4Responses.addAll(source.getServices().getServices());
        serviceDependencyMatrixV4Response.setServices(servicesV4Responses);

        return serviceDependencyMatrixV4Response;
    }
}

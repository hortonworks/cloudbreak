package com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.CmTemplateGeneratorConfigurationResolver;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.domain.dependencies.ServiceConfig;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.Dependencies;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.ServiceDependencyMatrix;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.Services;

@Service
public class ServiceDependencyMatrixService {

    @Inject
    private CmTemplateGeneratorConfigurationResolver resolver;

    public ServiceDependencyMatrix collectServiceDependencyMatrix(Set<String> services, String stackType, String version) {
        ServiceDependencyMatrix serviceDependencyMatrix = new ServiceDependencyMatrix();

        Services servicesObject = new Services();
        servicesObject.setServices(services);

        serviceDependencyMatrix.setServices(servicesObject);

        Dependencies dependencies = new Dependencies();
        Set<String> deps = new HashSet<>();
        for (String service : services) {
            for (ServiceConfig serviceInformation : resolver.serviceConfigs()) {
                if (service.toUpperCase(Locale.ROOT).equals(serviceInformation.getName())) {
                    for (String dependency : serviceInformation.getDependencies()) {
                        deps.add(dependency.toUpperCase(Locale.ROOT));
                    }
                    break;
                }
            }
        }
        dependencies.setServices(deps);
        serviceDependencyMatrix.setDependencies(dependencies);

        return serviceDependencyMatrix;
    }
}

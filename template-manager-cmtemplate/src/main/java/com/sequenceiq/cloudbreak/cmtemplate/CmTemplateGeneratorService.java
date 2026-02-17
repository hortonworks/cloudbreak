package com.sequenceiq.cloudbreak.cmtemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.ServiceDependencyMatrixService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.domain.ServiceDependencyMatrix;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.DeclaredVersionService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.SupportedVersionService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersions;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.GeneratedCmTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.domain.GeneratedCmTemplate;

@Service
public class CmTemplateGeneratorService {

    @Inject
    private SupportedVersionService supportedVersionService;

    @Inject
    private ServiceDependencyMatrixService serviceDependencyMatrixService;

    @Inject
    private GeneratedCmTemplateService generatedCMTemplateService;

    @Inject
    private DeclaredVersionService declaredVersionService;

    public GeneratedCmTemplate generateTemplateByServices(Set<String> services, String platform) {
        String generatedId = UUID.randomUUID().toString();
        String[] stackTypeAndVersionArray = getStackTypeAndVersion(platform);
        Set<String> dependentServices = serviceDependencyMatrixService
                .collectServiceDependencyMatrix(services, stackTypeAndVersionArray[0], stackTypeAndVersionArray[1])
                .getDependencies()
                .getServices();
        Set<String> servicesWithDependencies = new HashSet<>(dependentServices);
        servicesWithDependencies.addAll(services);

        return generatedCMTemplateService.prepareClouderaManagerTemplate(
                servicesWithDependencies,
                stackTypeAndVersionArray[0],
                stackTypeAndVersionArray[1],
                generatedId);
    }

    public ServiceDependencyMatrix getServicesAndDependencies(Set<String> services, String platform) {
        String[] stackTypeAndVersionArray = getStackTypeAndVersion(platform);
        return serviceDependencyMatrixService.collectServiceDependencyMatrix(
                services,
                stackTypeAndVersionArray[0],
                stackTypeAndVersionArray[1]);
    }

    private String[] getStackTypeAndVersion(String platform) {
        //Maybe the usage of a Pattern instead of simple split would be more elegant, especially if we support CDH versions only :)
        String[] stackTypeAndVersionArray = platform.split("-");
        if (stackTypeAndVersionArray.length != 2) {
            throw new BadRequestException("The request does not contain stack type and version or it does not match for the required pattern eg CDH-6.1.");
        }
        return stackTypeAndVersionArray;
    }

    public SupportedVersions getVersionsAndSupportedServiceList(Set<String> versions) {
        return supportedVersionService.collectSupportedVersions(versions);
    }

    public SupportedServices getServicesByBlueprint(String blueprintText) {
        return declaredVersionService.collectDeclaredVersions(blueprintText);
    }
}

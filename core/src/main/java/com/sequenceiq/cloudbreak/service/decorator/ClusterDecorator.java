package com.sequenceiq.cloudbreak.service.decorator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.AmbariHaComponentFilter;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class ClusterDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDecorator.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private AmbariHaComponentFilter ambariHaComponentFilter;

    @Measure(ClusterDecorator.class)
    public Cluster decorate(@Nonnull Cluster cluster, @Nonnull ClusterV4Request request, Blueprint blueprint, User user, Workspace workspace,
            @Nonnull Stack stack) {
        prepareBlueprint(cluster, request, workspace, stack, Optional.ofNullable(blueprint), user);
        prepareClusterManagerVariant(cluster);
        validateBlueprintIfRequired(cluster, request, stack);
        prepareRds(cluster, request, stack);
        cluster = sharedServiceConfigProvider.configureCluster(cluster, user, workspace);
        return cluster;
    }

    private void validateBlueprintIfRequired(Cluster subject, ClusterV4Request request, Stack stack) {
        if (blueprintService.isAmbariBlueprint(subject.getBlueprint()) && request.getValidateBlueprint()) {
            ambariBlueprintValidator.validateBlueprintForStack(subject, subject.getBlueprint(), subject.getHostGroups(), stack.getInstanceGroups());
        }
    }

    private void prepareBlueprint(Cluster subject, ClusterV4Request request, Workspace workspace, Stack stack,
            Optional<Blueprint> blueprint, User user) {
        if (blueprint.isPresent()) {
            subject.setBlueprint(blueprint.get());
        } else if (!Strings.isNullOrEmpty(request.getBlueprintName())) {
            subject.setBlueprint(blueprintService.getByNameForWorkspace(request.getBlueprintName(), workspace));
        } else {
            throw new BadRequestException("Cluster definition is not configured for the cluster!");
        }
        removeHaComponentsFromGatewayTopologies(subject);
        subject.setTopologyValidation(request.getValidateBlueprint());
    }

    private void prepareClusterManagerVariant(Cluster cluster) {
        cluster.setVariant(blueprintService.getBlueprintVariant(cluster.getBlueprint()));
    }

    // because KNOX does not support them
    private void removeHaComponentsFromGatewayTopologies(Cluster subject) {
        String blueprintText = subject.getBlueprint().getBlueprintText();
        Set<String> haComponents = ambariHaComponentFilter.getHaComponents(new AmbariBlueprintTextProcessor(blueprintText));
        Set<String> haKnoxServices = ExposedService.filterSupportedKnoxServices().stream()
                .filter(es -> haComponents.contains(es.getAmbariServiceName())
                        && !ExposedService.RANGER.getAmbariServiceName().equalsIgnoreCase(es.getAmbariServiceName()))
                .map(ExposedService::getKnoxService)
                .collect(Collectors.toSet());
        Gateway gateway = subject.getGateway();
        if (gateway != null) {
            gateway.getTopologies().forEach(topology -> {
                try {
                    Json exposedServices = topology.getExposedServices();
                    if (exposedServices != null) {
                        ExposedServices exposedServicesOfTopology = exposedServices.get(ExposedServices.class);
                        exposedServicesOfTopology.getServices().removeAll(haKnoxServices);
                        topology.setExposedServices(new Json(exposedServicesOfTopology));
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to read the gateway topologies and exposed services", e);
                }
            });
        }
    }

    private void prepareRds(Cluster subject, ClusterV4Request request, Stack stack) {
        subject.setRdsConfigs(new HashSet<>());
        Optional.ofNullable(request.getDatabases())
                .ifPresent(confs -> confs.forEach(confName -> subject.getRdsConfigs().add(
                        rdsConfigService.getByNameForWorkspace(confName, stack.getWorkspace()))));
    }
}

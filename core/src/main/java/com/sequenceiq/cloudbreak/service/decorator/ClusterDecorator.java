package com.sequenceiq.cloudbreak.service.decorator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.AmbariHaComponentFilter;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;

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
    private LdapConfigService ldapConfigService;

    @Inject
    private ClusterProxyDecorator clusterProxyDecorator;

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
        cluster = clusterProxyDecorator.prepareProxyConfig(cluster, request.getProxyName());
        prepareLdap(cluster, request, user, workspace);
        cluster = sharedServiceConfigProvider.configureCluster(cluster, user, workspace);
        return cluster;
    }

    private void prepareLdap(@Nonnull Cluster cluster, @Nonnull ClusterV4Request request, User user, Workspace workspace) {
        if (request.getLdapName() != null) {
            prepareLdap(cluster, workspace, request.getLdapName());
        }
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
                .filter(es -> haComponents.contains(es.getServiceName())
                        && !ExposedService.RANGER.getServiceName().equalsIgnoreCase(es.getServiceName()))
                .map(ExposedService::getKnoxService)
                .collect(Collectors.toSet());
        if (subject.getGateway() != null) {
            subject.getGateway().getTopologies().forEach(topology -> {
                try {
                    ExposedServices exposedServicesOfTopology = topology.getExposedServices().get(ExposedServices.class);
                    exposedServicesOfTopology.getServices().removeAll(haKnoxServices);
                    topology.setExposedServices(new Json(exposedServicesOfTopology));
                } catch (IOException e) {
                    LOGGER.error("This exception should never occur.", e);
                }
            });
        }
    }

    private void prepareLdap(Cluster cluster, Workspace workspace, @NotNull String ldapName) {
        LdapConfig ldapConfig = ldapConfigService.getByNameForWorkspace(ldapName, workspace);
        cluster.setLdapConfig(ldapConfig);
    }

    private void prepareRds(Cluster subject, ClusterV4Request request, Stack stack) {
        subject.setRdsConfigs(new HashSet<>());
        Optional.ofNullable(request.getDatabases())
                .ifPresent(confs -> confs.forEach(confName -> subject.getRdsConfigs().add(
                        rdsConfigService.getByNameForWorkspace(confName, stack.getWorkspace()))));
    }
}

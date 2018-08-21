package com.sequenceiq.cloudbreak.service.decorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.AmbariHaComponentFilter;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;

@Component
public class ClusterDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDecorator.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private ConversionService conversionService;

    @Inject
    private HostGroupDecorator hostGroupDecorator;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private ClusterProxyDecorator clusterProxyDecorator;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Inject
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Inject
    private AmbariHaComponentFilter ambariHaComponentFilter;

    public Cluster decorate(@Nonnull Cluster cluster, @Nonnull ClusterRequest request, Blueprint blueprint,
            Organization organization, @Nonnull Stack stack) {
        prepareBlueprint(cluster, request, organization, stack, Optional.ofNullable(blueprint));
        prepareHostGroups(stack, cluster, request.getHostGroups());
        validateBlueprintIfRequired(cluster, request, stack);
        prepareRds(cluster, request, stack);
        cluster = clusterProxyDecorator.prepareProxyConfig(cluster, request.getProxyName(), stack);
        prepareLdap(cluster, request, organization);
        cluster = sharedServiceConfigProvider.configureCluster(cluster, request.getConnectedCluster());
        return cluster;
    }

    private void prepareLdap(@Nonnull Cluster cluster, @Nonnull ClusterRequest request, Organization organization) {
        if (request.getLdapConfig() != null) {
            prepareLdap(cluster, organization, request.getLdapConfig());
        } else if (request.getLdapConfigName() != null) {
            prepareLdap(cluster, organization, request.getLdapConfigName());
        } else if (request.getLdapConfigId() != null) {
            prepareLdap(cluster, request.getLdapConfigId());
        }
    }

    private void validateBlueprintIfRequired(Cluster subject, ClusterRequest request, Stack stack) {
        if (request.getValidateBlueprint()) {
            blueprintValidator.validateBlueprintForStack(subject, subject.getBlueprint(), subject.getHostGroups(), stack.getInstanceGroups());
        }
    }

    private void prepareBlueprint(Cluster subject, ClusterRequest request, Organization organization, Stack stack,
            Optional<Blueprint> blueprint) {
        if (blueprint.isPresent()) {
            subject.setBlueprint(blueprint.get());
        } else {
            if (request.getBlueprintId() != null) {
                subject.setBlueprint(blueprintService.get(request.getBlueprintId()));
            } else if (request.getBlueprint() != null) {
                Blueprint newBlueprint = conversionService.convert(request.getBlueprint(), Blueprint.class);
                newBlueprint.setPublicInAccount(stack.isPublicInAccount());
                newBlueprint = blueprintService.create(organization, newBlueprint, new ArrayList<>());
                subject.setBlueprint(newBlueprint);
            } else if (!Strings.isNullOrEmpty(request.getBlueprintName())) {
                subject.setBlueprint(blueprintService.getByNameForOrganization(request.getBlueprintName(), organization));
            } else {
                throw new BadRequestException("Blueprint is not configured for the cluster!");
            }
        }
        removeHaComponentsFromGatewayTopologies(subject);
        subject.setTopologyValidation(request.getValidateBlueprint());
    }

    // because KNOX does not support them
    private void removeHaComponentsFromGatewayTopologies(Cluster subject) {
        Set<String> haComponents = ambariHaComponentFilter.getHaComponents(new BlueprintTextProcessor(subject.getBlueprint().getBlueprintText()));
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
                    LOGGER.warn("This exception should never occur.", e);
                }
            });
        }
    }

    private void prepareLdap(Cluster cluster, @NotNull Long ldapConfigId) {
        LdapConfig ldapConfig = ldapConfigService.get(ldapConfigId);
        cluster.setLdapConfig(ldapConfig);
    }

    private void prepareLdap(Cluster cluster, Organization organization, @NotNull LdapConfigRequest ldapConfigRequest) {
        LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest, LdapConfig.class);
        ldapConfigValidator.validateLdapConnection(ldapConfig);
        ldapConfig = ldapConfigService.create(ldapConfig, organization);
        cluster.setLdapConfig(ldapConfig);
    }

    private void prepareLdap(Cluster cluster, Organization organization, @NotNull String ldapName) {
        LdapConfig ldapConfig = ldapConfigService.getByNameForOrganization(ldapName, organization);
        cluster.setLdapConfig(ldapConfig);
    }

    private void prepareRds(Cluster subject, ClusterRequest request, Stack stack) {
        subject.setRdsConfigs(new HashSet<>());
        if (request.getRdsConfigIds() != null) {
            for (Long rdsConfigId : request.getRdsConfigIds()) {
                RDSConfig rdsConfig = rdsConfigService.get(rdsConfigId);
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
        if (request.getRdsConfigJsons() != null) {
            for (RDSConfigRequest requestRdsConfig : request.getRdsConfigJsons()) {
                RDSConfig rdsConfig = conversionService.convert(requestRdsConfig, RDSConfig.class);
                rdsConfig.setPublicInAccount(stack.isPublicInAccount());
                rdsConfig = rdsConfigService.createIfNotExists(stack.getCreator(), rdsConfig, stack.getOrganization().getId());
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
        Optional.of(request.getRdsConfigNames())
                .ifPresent(confs -> confs.forEach(confName -> subject.getRdsConfigs().add(
                        rdsConfigService.getByNameForOrganization(confName, stack.getOrganization()))));

        if (request.getAmbariDatabaseDetails() != null) {
            RDSConfig rdsConfig = ambariDatabaseMapper.mapAmbariDatabaseDetailsJsonToRdsConfig(request.getAmbariDatabaseDetails(), subject, stack,
                    stack.isPublicInAccount());
            subject.getRdsConfigs().add(rdsConfigService.createIfNotExists(stack.getCreator(), rdsConfig, stack.getOrganization().getId()));
        }
    }

    private void prepareHostGroups(Stack stack, Cluster cluster, Iterable<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, stack, true, stack.isPublicInAccount());
            hostGroups.add(hostGroup);
        }
        cluster.setHostGroups(hostGroups);
    }

}

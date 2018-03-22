package com.sequenceiq.cloudbreak.service.decorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariConfigurationService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDecorator.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private StackService stackService;

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
    private ClusterService clusterService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private ClusterProxyDecorator clusterProxyDecorator;

    @Inject
    private AmbariConfigurationService ambariConfigurationService;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    public Cluster decorate(Cluster subject, ClusterRequest request, Blueprint blueprint, IdentityUser user, Stack stack) {
        if (blueprint != null) {
            subject.setBlueprint(blueprint);
        } else {
            if (request.getBlueprintId() != null) {
                subject.setBlueprint(blueprintService.get(request.getBlueprintId()));
            } else if (request.getBlueprint() != null) {
                Blueprint newBlueprint = conversionService.convert(request.getBlueprint(), Blueprint.class);
                newBlueprint.setPublicInAccount(stack.isPublicInAccount());
                newBlueprint = blueprintService.create(user, newBlueprint, new ArrayList<>());
                subject.setBlueprint(newBlueprint);
            } else if (!Strings.isNullOrEmpty(request.getBlueprintName())) {
                subject.setBlueprint(blueprintService.get(request.getBlueprintName(), user.getAccount()));
            } else {
                throw new BadRequestException("Blueprint does not configured for the cluster!");
            }
        }
        subject.setHostGroups(convertHostGroupsFromJson(stack, user, subject, request.getHostGroups()));
        if (request.getValidateBlueprint()) {
            blueprintValidator.validateBlueprintForStack(subject, subject.getBlueprint(), subject.getHostGroups(), stack.getInstanceGroups());
        }
        subject.setTopologyValidation(request.getValidateBlueprint());
        prepareRds(subject, user, request, stack);
        subject = clusterProxyDecorator.prepareProxyConfig(subject, user, request.getProxyName(), stack);
        prepareLdap(subject, user, request.getLdapConfigId(), request.getLdapConfig(), request.getLdapConfigName(), stack);
        prepareConnectedClusterParameters(subject, user, request.getConnectedCluster());
        return subject;
    }

    private void prepareConnectedClusterParameters(Cluster requestedCluster, IdentityUser user, ConnectedClusterRequest connectedClusterRequest) {
        if (connectedClusterRequest != null) {
            Long stackId;
            Stack publicStack;
            if (!Strings.isNullOrEmpty(connectedClusterRequest.getSourceClusterName())) {
                publicStack = stackService.getPublicStack(connectedClusterRequest.getSourceClusterName(), user);
                stackId = publicStack.getId();
            } else {
                stackId = connectedClusterRequest.getSourceClusterId();
                publicStack = stackService.get(connectedClusterRequest.getSourceClusterId());
            }
            // We should set the ldap to the source cluster ldap
            requestedCluster.setLdapConfig(publicStack.getCluster().getLdapConfig());
            // We should set the ranger metastore to the source cluster ranger metastore if exist!
            RDSConfig rangerRds = rdsConfigService.findByClusterIdAndType(publicStack.getOwner(), publicStack.getAccount(),
                    publicStack.getCluster().getId(), RdsType.RANGER);
            if (rangerRds != null) {
                requestedCluster.getRdsConfigs().add(rangerRds);
            }

            try {

                Set<BlueprintParameterJson> requests = new HashSet<>();
                Json blueprintAttributes = requestedCluster.getBlueprint().getInputParameters();
                if (blueprintAttributes != null && StringUtils.isNoneEmpty(blueprintAttributes.getValue())) {
                    BlueprintInputParameters inputParametersObj = blueprintAttributes.get(BlueprintInputParameters.class);
                    for (BlueprintParameter blueprintParameter : inputParametersObj.getParameters()) {
                        BlueprintParameterJson blueprintParameterJson = new BlueprintParameterJson();
                        blueprintParameterJson.setName(blueprintParameter.getName());
                        blueprintParameterJson.setReferenceConfiguration(blueprintParameter.getReferenceConfiguration());
                        blueprintParameterJson.setDescription(blueprintParameter.getDescription());
                        requests.add(blueprintParameterJson);
                    }
                }
                ConfigsResponse configsResponse = clusterService.retrieveOutputs(stackId, requests);

                Map<String, String> newInputs = requestedCluster.getBlueprintInputs().get(Map.class);
                for (BlueprintInputJson blueprintInputJson : configsResponse.getInputs()) {
                    newInputs.put(blueprintInputJson.getName(), blueprintInputJson.getPropertyValue());
                }
                requestedCluster.setBlueprintInputs(new Json(newInputs));
            } catch (IOException e) {
                LOGGER.error("Could not propagate cluster input parameters", e);
                throw new BadRequestException("Could not propagate cluster input parameters: " + e.getMessage());
            }
        }
    }

    private void prepareLdap(Cluster subject, IdentityUser user, Long ldapConfigId, LdapConfigRequest ldapConfigRequest, String ldapName, Stack stack) {
        if (ldapConfigId != null) {
            LdapConfig ldapConfig = ldapConfigService.get(ldapConfigId);
            subject.setLdapConfig(ldapConfig);
        } else if (ldapName != null) {
            LdapConfig ldapConfig = ldapConfigService.getPublicConfig(ldapName, user);
            subject.setLdapConfig(ldapConfig);
        } else if (ldapConfigRequest != null) {
            LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest, LdapConfig.class);
            ldapConfig.setPublicInAccount(stack.isPublicInAccount());
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            ldapConfig = ldapConfigService.create(user, ldapConfig);
            subject.setLdapConfig(ldapConfig);
        }
    }

    private void prepareRds(Cluster subject, IdentityUser user, ClusterRequest request, Stack stack) {
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
                rdsConfig = rdsConfigService.createIfNotExists(user, rdsConfig);
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
        Optional.of(request.getRdsConfigNames())
                .ifPresent(confs -> confs.forEach(confName -> subject.getRdsConfigs().add(rdsConfigService.getPublicRdsConfig(confName, user))));

        if (request.getAmbariDatabaseDetails() != null) {
            RDSConfig rdsConfig = ambariDatabaseMapper.mapAmbariDatabaseDetailsJsonToRdsConfig(request.getAmbariDatabaseDetails(), stack);
            subject.getRdsConfigs().add(rdsConfigService.createIfNotExists(user, rdsConfig));
        }

        ambariConfigurationService.createDefaultRdsConfigIfNeeded(stack, subject).ifPresent(rdsConfig -> subject.getRdsConfigs().add(rdsConfig));
    }

    private Set<HostGroup> convertHostGroupsFromJson(Stack stack, IdentityUser user, Cluster cluster, Iterable<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, user, stack.getId(), true, stack.isPublicInAccount());
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

}

package com.sequenceiq.cloudbreak.service.decorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
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
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterDecorator implements Decorator<Cluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDecorator.class);

    private enum DecorationData {
        STACK_ID,
        USER,
        BLUEPRINT_ID,
        HOSTGROUP_JSONS,
        VALIDATE_BLUEPRINT,
        RDSCONFIG_ID,
        LDAP_CONFIG_ID,
        BLUEPRINT,
        RDSCONFIG,
        LDAP_CONFIG,
        CONNECTED_CLUSTER,
        BLUEPRINT_NAME
    }

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

    @Override
    public Cluster decorate(Cluster subject, Object... data) {
        if (null == data || data.length != DecorationData.values().length) {
            throw new IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.getName());
        }
        Long stackId = (Long) data[DecorationData.STACK_ID.ordinal()];
        IdentityUser user = (IdentityUser) data[DecorationData.USER.ordinal()];
        Long blueprintId = (Long) data[DecorationData.BLUEPRINT_ID.ordinal()];
        Long ldapConfigId = (Long) data[DecorationData.LDAP_CONFIG_ID.ordinal()];
        Set<HostGroupRequest> hostGroupsJsons = (Set<HostGroupRequest>) data[DecorationData.HOSTGROUP_JSONS.ordinal()];
        BlueprintRequest requestBlueprint = (BlueprintRequest) data[DecorationData.BLUEPRINT.ordinal()];
        Set<RDSConfigRequest> requestRdsConfigs = (Set<RDSConfigRequest>) data[DecorationData.RDSCONFIG.ordinal()];
        LdapConfigRequest ldapConfigRequest = (LdapConfigRequest) data[DecorationData.LDAP_CONFIG.ordinal()];
        Set<Long> rdsConfigIds = (Set<Long>) data[DecorationData.RDSCONFIG_ID.ordinal()];
        ConnectedClusterRequest connectedClusterRequest = (ConnectedClusterRequest) data[DecorationData.CONNECTED_CLUSTER.ordinal()];
        String blueprintName = (String) data[DecorationData.BLUEPRINT_NAME.ordinal()];

        Stack stack = stackService.getById(stackId);

        if (blueprintId != null) {
            subject.setBlueprint(blueprintService.get(blueprintId));
        } else if (requestBlueprint != null) {
            Blueprint blueprint = conversionService.convert(requestBlueprint, Blueprint.class);
            blueprint.setPublicInAccount(stack.isPublicInAccount());
            blueprint = blueprintService.create(user, blueprint, new ArrayList<>());
            subject.setBlueprint(blueprint);
        } else if (!Strings.isNullOrEmpty(blueprintName)) {
            subject.setBlueprint(blueprintService.getByName(blueprintName, user));
        } else {
            throw new BadRequestException("Blueprint does not configured for the cluster!");
        }
        subject.setHostGroups(convertHostGroupsFromJson(stack, user, subject, hostGroupsJsons));
        boolean validate = (boolean) data[DecorationData.VALIDATE_BLUEPRINT.ordinal()];
        if (validate) {
            Blueprint blueprint = blueprintId != null ? blueprintService.get(blueprintId) : subject.getBlueprint();
            blueprintValidator.validateBlueprintForStack(blueprint, subject.getHostGroups(), stack.getInstanceGroups());
        }
        subject.setTopologyValidation(validate);
        prepareRds(subject, user, rdsConfigIds, requestRdsConfigs, stack);
        prepareLdap(subject, user, ldapConfigId, ldapConfigRequest, stack);
        prepareConnectedClusterParameters(subject, user, connectedClusterRequest);
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
            } catch (IOException | CloudbreakSecuritySetupException e) {
                LOGGER.error("Could not propagate cluster input parameters: {}", e);
                throw new BadRequestException("Could not propagate cluster input parameters: " + e.getMessage());
            }
        }
    }

    private void prepareLdap(Cluster subject, IdentityUser user, Long ldapConfigId, LdapConfigRequest ldapConfigRequest, Stack stack) {
        if (ldapConfigId != null) {
            LdapConfig ldapConfig = ldapConfigService.get(ldapConfigId);
            subject.setLdapConfig(ldapConfig);
        } else if (ldapConfigRequest != null) {
            LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest, LdapConfig.class);
            ldapConfig.setPublicInAccount(stack.isPublicInAccount());
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            ldapConfig = ldapConfigService.create(user, ldapConfig);
            subject.setLdapConfig(ldapConfig);
        }
    }

    private void prepareRds(Cluster subject, IdentityUser user, Set<Long> rdsConfigIds, Set<RDSConfigRequest> requestRdsConfigs, Stack stack) {
        subject.setRdsConfigs(new HashSet<>());
        if (rdsConfigIds != null && !rdsConfigIds.isEmpty()) {
            for (Long rdsConfigId : rdsConfigIds) {
                RDSConfig rdsConfig = rdsConfigService.get(rdsConfigId);
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
        if (requestRdsConfigs != null && !requestRdsConfigs.isEmpty()) {
            for (RDSConfigRequest requestRdsConfig : requestRdsConfigs) {
                if (requestRdsConfig.isValidated()) {
                    rdsConnectionValidator.validateRdsConnection(
                            requestRdsConfig.getConnectionURL(),
                            requestRdsConfig.getConnectionUserName(),
                            requestRdsConfig.getConnectionPassword());
                }
                RDSConfig rdsConfig = conversionService.convert(requestRdsConfig, RDSConfig.class);
                rdsConfig.setPublicInAccount(stack.isPublicInAccount());
                rdsConfig = rdsConfigService.createIfNotExists(user, rdsConfig);
                subject.getRdsConfigs().add(rdsConfig);
            }
        }
    }

    private Set<HostGroup> convertHostGroupsFromJson(Stack stack, IdentityUser user, Cluster cluster, Set<HostGroupRequest> hostGroupsJsons) {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : hostGroupsJsons) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup.setCluster(cluster);
            hostGroup = hostGroupDecorator.decorate(hostGroup, stack.getId(), user, json.getConstraint(), json.getRecipeIds(),
                    true, json.getRecipes(), stack.isPublicInAccount(), json.getRecipeNames());
            hostGroups.add(hostGroup);
        }
        return hostGroups;
    }

}

package com.sequenceiq.cloudbreak.service.sharedservice;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;
import com.sequenceiq.cloudbreak.service.cluster.KerberosConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.service.credential.CredentialPrerequisiteService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class SharedServiceConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedServiceConfigProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private KerberosConfigProvider kerberosConfigProvider;

    @Inject
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Inject
    private DatalakeResourcesRepository datalakeResourcesRepository;

    @Inject
    private CredentialPrerequisiteService credentialPrerequisiteService;

    public Cluster configureCluster(@Nonnull Cluster requestedCluster, User user, Workspace workspace) {
        Objects.requireNonNull(requestedCluster);
        Stack stack = requestedCluster.getStack();
        if (stack.getDatalakeResourceId() != null) {
            Optional<DatalakeResources> datalakeResources = datalakeResourcesRepository.findById(stack.getDatalakeResourceId());
            if (datalakeResources.isPresent()) {
                DatalakeResources datalakeResource = datalakeResources.get();
                setupLdap(requestedCluster, datalakeResource);
                setupRds(requestedCluster, datalakeResource);
                kerberosConfigProvider.setKerberosConfigForWorkloadCluster(requestedCluster, datalakeResource);
            }
        }
        return requestedCluster;
    }

    public Stack prepareDatalakeConfigs(Stack publicStack) {
        try {
            AmbariClient ambariClient = null;
            Optional<DatalakeResources> datalakeResource = Optional.empty();
            if (publicStack.getDatalakeResourceId() != null || (!CollectionUtils.isEmpty(publicStack.getEnvironment().getDatalakeResources())
                    && publicStack.getEnvironment().getDatalakeResources().size() == 1)) {
                Long datalakeResourceId = getDatalakeResourceIdFromEnvOrStack(publicStack);
                datalakeResource = datalakeResourcesRepository.findById(datalakeResourceId);
                if (credentialPrerequisiteService.isCumulusCredential(publicStack.getCredential().getAttributes())) {
                    ambariClient = credentialPrerequisiteService.createCumulusAmbariClient(publicStack.getCredential().getAttributes());
                }
            }
            Long datalakeStackId = getDatalakeStackId(publicStack, datalakeResource);
            if (datalakeStackId != null) {
                Stack datalakeStack = stackService.getById(datalakeStackId);
                ambariClient = ambariClientFactory.getAmbariClient(datalakeStack, datalakeStack.getCluster());
                if (!datalakeResource.isPresent()) {
                    datalakeResource = Optional.of(
                            ambariDatalakeConfigProvider.collectAndStoreDatalakeResources(datalakeStack, datalakeStack.getCluster(), ambariClient));
                }
            }
            if (decorateStackWithConfigs(publicStack, ambariClient, datalakeResource)) {
                return stackService.save(publicStack);
            }
            return publicStack;
        } catch (IOException e) {
            LOGGER.warn("Could not propagate cluster input parameters");
            throw new BadRequestException("Could not propagate cluster input parameters: " + e.getMessage(), e);
        }
    }

    private boolean decorateStackWithConfigs(Stack publicStack, AmbariClient ambariClient, Optional<DatalakeResources> datalakeResource) throws IOException {
        if (datalakeResource.isPresent() && ambariClient != null) {
            DatalakeResources datalakeResources = datalakeResource.get();
            publicStack.setDatalakeResourceId(datalakeResources.getId());
            Map<String, String> additionalParams = ambariDatalakeConfigProvider.getAdditionalParameters(publicStack, datalakeResources);
            Map<String, String> blueprintConfigParams =
                    ambariDatalakeConfigProvider.getBlueprintConfigParameters(datalakeResources, publicStack, ambariClient);
            StackInputs stackInputs = publicStack.getInputs().get(StackInputs.class);
            stackInputs.setDatalakeInputs((Map) blueprintConfigParams);
            stackInputs.setFixInputs((Map) additionalParams);
            try {
                publicStack.setInputs(new Json(stackInputs));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("An error occured under the stackinput persistence which cause a stack creation problem", e);
            }
            return true;
        }
        return false;
    }

    private Long getDatalakeResourceIdFromEnvOrStack(Stack publicStack) {
        return publicStack.getDatalakeResourceId() != null
                ? publicStack.getDatalakeResourceId() : publicStack.getEnvironment().getDatalakeResources().stream().findFirst().get().getId();
    }

    private Long getDatalakeStackId(Stack publicStack, Optional<DatalakeResources> datalakeResource) {
        return datalakeResource.isPresent() ? datalakeResource.get().getDatalakeStackId() : null;
    }

    private void addDatalakeRequiredProperties(Set<String> datalakeProperties) {
        datalakeProperties.add("ranger.audit.solr.zookeepers");
        datalakeProperties.add("atlas.rest.address");
        datalakeProperties.add("atlas.kafka.bootstrap.servers");
        datalakeProperties.add("atlas.kafka.security.protocol");
        datalakeProperties.add("atlas.jaas.KafkaClient.option.serviceName");
        datalakeProperties.add("atlas.kafka.sasl.kerberos.service.name");
        datalakeProperties.add("atlas.kafka.zookeeper.connect");
        datalakeProperties.add("ranger_admin_username");
        datalakeProperties.add("policymgr_external_url");
    }

    public Map<String, Object> prepareAdditionalInputParameters(String sourceClustername, String clusterName) {
        Map<String, Object> result = new HashMap<>();
        result.put("REMOTE_CLUSTER_NAME", sourceClustername);
        result.put("remoteClusterName", sourceClustername);
        result.put("remote.cluster.name", sourceClustername);
        result.put("cluster_name", clusterName);
        result.put("cluster.name", clusterName);
        return result;
    }

    private void setupRds(Cluster requestedCluster, DatalakeResources datalakeResources) {
        if (requestedCluster.getRdsConfigs().isEmpty() && datalakeResources.getRdsConfigs() != null) {
            requestedCluster.getRdsConfigs().addAll(
                    datalakeResources.getRdsConfigs()
                            .stream()
                            .filter(rdsConfig -> ResourceStatus.USER_MANAGED == rdsConfig.getStatus())
                            .collect(toSet()));
        }
    }

    private Stack queryStack(Long sourceClusterId, Optional<String> sourceClusterName, User user, Workspace workspace) {
        return sourceClusterName.isPresent()
                ? stackService.getByNameInWorkspace(sourceClusterName.get(), workspace.getId())
                : stackService.getById(sourceClusterId);
    }

    private void setupLdap(Cluster requestedCluster, DatalakeResources datalakeResources) {
        requestedCluster.setLdapConfig(datalakeResources.getLdapConfig());
    }
}

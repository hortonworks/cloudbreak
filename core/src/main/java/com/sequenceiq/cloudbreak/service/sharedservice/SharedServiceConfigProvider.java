package com.sequenceiq.cloudbreak.service.sharedservice;

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

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class SharedServiceConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedServiceConfigProvider.class);

    @Inject
    private StackService stackService;

    @Inject
    private AmbariDatalakeConfigProvider ambariDatalakeConfigProvider;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private RemoteDataContextWorkaroundService remoteDataContextWorkaroundService;

    public Cluster configureCluster(@Nonnull Cluster requestedCluster, User user, Workspace workspace) {
        Objects.requireNonNull(requestedCluster);
        Stack stack = requestedCluster.getStack();
        if (stack.getDatalakeResourceId() != null) {
            Optional<DatalakeResources> datalakeResources = datalakeResourcesService.findById(stack.getDatalakeResourceId());
            if (datalakeResources.isPresent()) {
                DatalakeResources datalakeResource = datalakeResources.get();
                setupRds(requestedCluster, datalakeResource);
                setupStoragePath(requestedCluster, datalakeResource);
            }
        }
        return requestedCluster;
    }

    @Measure(SharedServiceConfigProvider.class)
    public Stack prepareDatalakeConfigs(Stack publicStack) {
        try {
            Optional<DatalakeResources> datalakeResource = Optional.empty();
            Set<DatalakeResources> datalakeResources = datalakeResourcesService
                    .findDatalakeResourcesByWorkspaceAndEnvironment(publicStack.getWorkspace().getId(), publicStack.getEnvironmentCrn());
            if (publicStack.getDatalakeResourceId() != null || (!CollectionUtils.isEmpty(datalakeResources) && datalakeResources.size() == 1)) {
                Long datalakeResourceId = getDatalakeResourceIdFromEnvOrStack(publicStack, datalakeResources);
                datalakeResource = datalakeResourcesService.findById(datalakeResourceId);
            }
            if (decorateStackWithConfigs(publicStack, datalakeResource)) {
                return stackService.save(publicStack);
            }
            return publicStack;
        } catch (IOException e) {
            LOGGER.warn("Could not propagate cluster input parameters");
            throw new BadRequestException("Could not propagate cluster input parameters: " + e.getMessage(), e);
        }
    }

    private boolean decorateStackWithConfigs(Stack publicStack, Optional<DatalakeResources> datalakeResource) throws IOException {
        if (datalakeResource.isPresent()) {
            DatalakeResources datalakeResources = datalakeResource.get();
            publicStack.setDatalakeResourceId(datalakeResources.getId());
            Map<String, String> additionalParams = ambariDatalakeConfigProvider.getAdditionalParameters(publicStack, datalakeResources);
            StackInputs stackInputs = publicStack.getInputs().get(StackInputs.class);
            stackInputs.setDatalakeInputs(new HashMap<>());
            stackInputs.setFixInputs((Map) additionalParams);
            try {
                publicStack.setInputs(new Json(stackInputs));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("An error occured under the stackinput persistence which cause a stack creation problem", e);
            }
            return true;
        }
        return false;
    }

    private Long getDatalakeResourceIdFromEnvOrStack(Stack publicStack, Set<DatalakeResources> datalakeResources) {
        return publicStack.getDatalakeResourceId() != null
                ? publicStack.getDatalakeResourceId() : datalakeResources.stream().findFirst().get().getId();
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
            requestedCluster.setRdsConfigs(remoteDataContextWorkaroundService.prepareRdsConfigs(requestedCluster, datalakeResources));
        }
    }

    private void setupStoragePath(Cluster requestedCluster, DatalakeResources datalakeResources) {
        FileSystem fileSystem = remoteDataContextWorkaroundService.prepareFilesytem(requestedCluster, datalakeResources);
        requestedCluster.setFileSystem(fileSystem);
    }

    private Stack queryStack(Long sourceClusterId, Optional<String> sourceClusterName, User user, Workspace workspace) {
        return sourceClusterName.isPresent()
                ? stackService.getByNameInWorkspace(sourceClusterName.get(), workspace.getId())
                : stackService.getById(sourceClusterId);
    }
}

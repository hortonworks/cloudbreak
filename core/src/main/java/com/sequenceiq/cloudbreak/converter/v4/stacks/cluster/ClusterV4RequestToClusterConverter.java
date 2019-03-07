package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.customcontainer.CustomContainerV4Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClouderaManagerProductV4RequestToClouderaManagerProductConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.domain.ClusterAttributes;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class ClusterV4RequestToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterV4Request, Cluster> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV4RequestToClusterConverter.class);

    @Value("${cb.ambari.username:cloudbreak}")
    private String ambariUserName;

    @Value("${cb.ambari.dp.username:dpapps}")
    private String dpUsername;

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private KerberosService kerberosService;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Override
    public Cluster convert(ClusterV4Request source) {
        Workspace workspace = workspaceService.getForCurrentUser();

        Cluster cluster = new Cluster();
        cluster.setName(source.getName());
        cluster.setStatus(REQUESTED);
        cluster.setUserName(source.getUserName());
        cluster.setPassword(source.getPassword());
        cluster.setExecutorType(source.getExecutorType());
        convertGateway(source, cluster);

        if (source.getKerberosName() != null) {
            KerberosConfig kerberosConfig = kerberosService.getByNameForWorkspaceId(source.getKerberosName(), workspace.getId());
            cluster.setKerberosConfig(kerberosConfig);
        }
        cluster.setCloudbreakAmbariUser(ambariUserName);
        cluster.setCloudbreakAmbariPassword(PasswordUtil.generatePassword());
        cluster.setDpAmbariUser(dpUsername);
        cluster.setDpAmbariPassword(PasswordUtil.generatePassword());
        cluster.setClusterDefinition(getClusterDefinition(source.getClusterDefinitionName(), workspace));
        if (cloudStorageValidationUtil.isCloudStorageConfigured(source.getCloudStorage())) {
            cluster.setFileSystem(getConversionService().convert(source.getCloudStorage(), FileSystem.class));
        }
        convertAttributes(source, cluster);
        try {
            Json json = new Json(convertContainerConfigs(source.getCustomContainer()));
            cluster.setCustomContainerDefinition(json);
        } catch (JsonProcessingException ignored) {
            cluster.setCustomContainerDefinition(null);
        }
        updateDatabases(source, cluster, workspace);
        convertAmbariSpecificPart(source, cluster);
        extractClusterManagerAndHdpRepoConfig(cluster, source);
        cluster.setProxyConfig(getProxyConfig(source.getProxyName(), workspace));
        cluster.setLdapConfig(getLdap(source.getLdapName(), workspace));
        return cluster;
    }

    private void convertAmbariSpecificPart(ClusterV4Request source, Cluster cluster) {
        if (source.getAmbari() != null) {
            cluster.setConfigStrategy(source.getAmbari().getConfigStrategy());
            cluster.setAmbariSecurityMasterKey(source.getAmbari().getSecurityMasterKey());
        }
    }

    private void convertGateway(ClusterV4Request source, Cluster cluster) {
        if (source.getGateway() != null) {
            if (StringUtils.isEmpty(source.getGateway().getPath())) {
                source.getGateway().setPath(source.getName());
            }
            Gateway gateway = getConversionService().convert(source.getGateway(), Gateway.class);
            if (gateway != null) {
                cluster.setGateway(gateway);
                gateway.setCluster(cluster);
            }
        }
    }

    private void convertAttributes(ClusterV4Request source, Cluster cluster) {
        Map<String, Object> attributesMap = source.getCustomQueue() != null
                ? Collections.singletonMap(ClusterAttributes.CUSTOM_QUEUE.name(), source.getCustomQueue())
                : Collections.emptyMap();
        try {
            cluster.setAttributes((new Json(attributesMap)).getValue());
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not initiate the attribute map on cluster object: ", e);
            throw new CloudbreakApiException("Failed to store exposedServices", e);
        }
    }

    private Map<String, String> convertContainerConfigs(CustomContainerV4Request customContainerRequest) {
        Map<String, String> configs = new HashMap<>();
        if (customContainerRequest != null) {
            configs.putAll(customContainerRequest.getDefinitions());
        }
        return configs;
    }

    private void extractClusterManagerAndHdpRepoConfig(Cluster cluster, ClusterV4Request clusterRequest) {
        Set<ClusterComponent> components = new HashSet<>();

        AmbariV4Request ambariRequest = clusterRequest.getAmbari();
        ClouderaManagerV4Request clouderaManagerV4Request = clusterRequest.getCm();
        if (Objects.nonNull(ambariRequest) && Objects.nonNull(clouderaManagerV4Request)) {
            throw new BadRequestException("Cannot determine cluster manager. More than one provided");
        }

        Optional.ofNullable(ambariRequest)
                .map(AmbariV4Request::getRepository)
                .map(ambariRepoRequest -> getConversionService().convert(ambariRepoRequest, AmbariRepo.class))
                .map(toJsonWrapException())
                .map(ambariRepoJson -> new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, ambariRepoJson, cluster))
                .ifPresent(components::add);

        Optional.ofNullable(ambariRequest)
                .map(AmbariV4Request::getStackRepository)
                .map(stackRepoRequest -> getConversionService().convert(stackRepoRequest, StackRepoDetails.class))
                .map(toJsonWrapException())
                .map(ambariRepoJson -> new ClusterComponent(ComponentType.HDP_REPO_DETAILS, ambariRepoJson, cluster))
                .ifPresent(components::add);

        Optional.ofNullable(clouderaManagerV4Request)
                .map(ClouderaManagerV4Request::getRepository)
                .map(ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter::convert)
                .map(toJsonWrapException())
                .map(cmRepoJson -> new ClusterComponent(ComponentType.CM_REPO_DETAILS, cmRepoJson, cluster))
                .ifPresent(components::add);

        Optional.ofNullable(clouderaManagerV4Request)
                .map(ClouderaManagerV4Request::getProducts)
                .orElseGet(List::of)
                .stream()
                .map(ClouderaManagerProductV4RequestToClouderaManagerProductConverter::convert)
                .map(toJsonWrapException())
                .map(cmRepoJson -> new ClusterComponent(ComponentType.CDH_PRODUCT_DETAILS, cmRepoJson, cluster))
                .forEach(components::add);

        cluster.setComponents(components);
    }

    private <T> Function<T, Json> toJsonWrapException() {
        return repositoryObject -> {
            try {
                return new Json(repositoryObject);
            } catch (IOException e) {
                throw new BadRequestException("Cannot serialize the stack template", e);
            }
        };
    }

    private ClusterDefinition getClusterDefinition(String clusterDefinitionName, Workspace workspace) {
        ClusterDefinition clusterDefinition = null;
        if (!StringUtils.isEmpty(clusterDefinitionName)) {
            clusterDefinition = clusterDefinitionService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(clusterDefinitionName, workspace);
            if (clusterDefinition == null) {
                throw new NotFoundException("Cluster definition does not exists by name: " + clusterDefinitionName);
            }
        }
        return clusterDefinition;
    }

    private void updateDatabases(ClusterV4Request source, Cluster cluster, Workspace workspace) {
        Set<String> databases = source.getDatabases();
        if (!CollectionUtils.isEmpty(databases)) {
            Set<RDSConfig> rdsConfigs = rdsConfigService.findByNamesInWorkspace(databases, workspace.getId());
            if (rdsConfigs.isEmpty()) {
                throw new NotFoundException("RDS config names dont exists");
            }
            cluster.setRdsConfigs(rdsConfigs);
        }
    }

    private ProxyConfig getProxyConfig(String proxyName, Workspace workspace) {
        if (StringUtils.isNotBlank(proxyName)) {
            return proxyConfigService.getByNameForWorkspace(proxyName, workspace);
        }
        return null;
    }

    private LdapConfig getLdap(String ldapName, Workspace workspace) {
        if (ldapName != null) {
            return ldapConfigService.getByNameForWorkspace(ldapName, workspace);
        }
        return null;
    }
}

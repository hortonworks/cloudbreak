package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class ClusterV2RequestToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterV2Request, Cluster> {

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private GatewayConvertUtil gatewayConvertUtil;

    @Inject
    private KerberosService kerberosService;

    @Override
    public Cluster convert(ClusterV2Request source) {
        Cluster cluster = new Cluster();
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        cluster.setExecutorType(source.getExecutorType());
        if (cloudStorageValidationUtil.isCloudStorageConfigured(source.getCloudStorage())) {
            cluster.setFileSystem(getConversionService().convert(source.getCloudStorage(), FileSystem.class));
        }
        cluster.setName(source.getName());
        if (!source.getRdsConfigNames().isEmpty()) {
            Set<RDSConfig> rdsConfigs = rdsConfigService.findByNamesInWorkspace(source.getRdsConfigNames(), workspace.getId());
            if (rdsConfigs.isEmpty()) {
                throw new NotFoundException("RDS config names dont exists");
            }
            cluster.setRdsConfigs(rdsConfigs);
        }
        cluster.setProxyConfig(getProxyConfig(source.getProxyName(), workspace));
        if (source.getLdapConfigName() != null) {
            cluster.setLdapConfig(ldapConfigService.getByNameForWorkspace(source.getLdapConfigName(), workspace));
        }
        AmbariV2Request ambariRequest = source.getAmbari();
        if (ambariRequest != null) {
            cluster.setClusterDefinition(getBlueprint(ambariRequest, workspace));
            cluster.setConfigStrategy(ambariRequest.getConfigStrategy());
            if (ambariRequest.getGateway() != null) {
                Gateway gateway = getConversionService().convert(ambariRequest.getGateway(), Gateway.class);
                cluster.setGateway(gateway);
                gateway.setWorkspace(workspace);
                gatewayConvertUtil.setGatewayPathAndSsoProvider(cluster.getName(), ambariRequest.getGateway(), gateway);
            }
            if (ambariRequest.getKerberosConfigName() != null) {
                KerberosConfig kerberosConfig = kerberosService.getByNameForWorkspaceId(ambariRequest.getKerberosConfigName(), workspace.getId());
                cluster.setKerberosConfig(kerberosConfig);
            }
            cluster.setPassword(ambariRequest.getPassword());
            cluster.setUserName(ambariRequest.getUserName());
            cluster.setAmbariSecurityMasterKey(ambariRequest.getAmbariSecurityMasterKey());
            extractAmbariAndHdpRepoConfig(cluster, source.getAmbari());
        }
        cluster.setWorkspace(workspace);
        return cluster;
    }

    private void extractAmbariAndHdpRepoConfig(Cluster cluster, AmbariV2Request ambariV2Request) {
        try {
            Set<ClusterComponent> components = new HashSet<>();
            if (ambariV2Request.getAmbariRepoDetailsJson() != null) {
                AmbariRepo ambariRepo = getConversionService().convert(ambariV2Request.getAmbariRepoDetailsJson(), AmbariRepo.class);
                components.add(new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, new Json(ambariRepo), cluster));
            }
            if (ambariV2Request.getAmbariStackDetails() != null) {
                StackRepoDetails stackRepoDetails = getConversionService().convert(ambariV2Request.getAmbariStackDetails(), StackRepoDetails.class);
                components.add(new ClusterComponent(ComponentType.HDP_REPO_DETAILS, new Json(stackRepoDetails), cluster));
            }
            cluster.setComponents(components);
        } catch (IOException e) {
            throw new BadRequestException("Cannot serialize the stack template", e);
        }
    }

    private ProxyConfig getProxyConfig(String proxyName, Workspace workspace) {
        if (StringUtils.isNotBlank(proxyName)) {
            return proxyConfigService.getByNameForWorkspace(proxyName, workspace);
        }
        return null;
    }

    private ClusterDefinition getBlueprint(AmbariV2Request ambariV2Request, Workspace workspace) {
        ClusterDefinition clusterDefinition = null;
        if (!StringUtils.isEmpty(ambariV2Request.getBlueprintName())) {
            clusterDefinition = clusterDefinitionService.getByNameForWorkspace(ambariV2Request.getBlueprintName(), workspace);
            if (clusterDefinition == null) {
                throw new NotFoundException("Blueprint does not exists by name: " + ambariV2Request.getBlueprintName());
            }
        }
        return clusterDefinition;
    }
}

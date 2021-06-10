package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.general.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.identitymapping.AwsMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.AzureMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.GcpMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.DatalakeView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class StackV4RequestToTemplatePreparationObjectConverter extends AbstractConversionServiceAwareConverter<StackV4Request, TemplatePreparationObject> {

    private static final int SERVER_PORT = 636;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private BlueprintViewProvider blueprintViewProvider;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Inject
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Inject
    private GcpMockAccountMappingService gcpMockAccountMappingService;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Inject
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Override
    public TemplatePreparationObject convert(StackV4Request source) {
        try {
            CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
            User user = userService.getOrCreate(cloudbreakUser);
            Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(source.getEnvironmentCrn());
            Credential credential = getCredential(source, environment);
            LdapView ldapConfig = getLdapConfig(source, environment);
            BaseFileSystemConfigurationsView fileSystemConfigurationView = getFileSystemConfigurationView(source, credential.getAttributes());
            Set<RDSConfig> rdsConfigs = getRdsConfigs(source, workspace);
            Blueprint blueprint = getBlueprint(source, workspace);
            Set<HostgroupView> hostgroupViews = getHostgroupViews(source);
            Gateway gateway = source.getCluster().getGateway() == null ? null : getConversionService().convert(source, Gateway.class);
            BlueprintView blueprintView = blueprintViewProvider.getBlueprintView(blueprint);
            GeneralClusterConfigs generalClusterConfigs = generalClusterConfigsProvider.generalClusterConfigs(source, cloudbreakUser.getEmail(),
                    blueprintService.getBlueprintVariant(blueprint));
            String gatewaySignKey = null;
            if (gateway != null) {
                gatewaySignKey = gateway.getSignKey();
            }
            String envCrnForVirtualGroups = getEnvironmentCrnForVirtualGroups(environment);
            VirtualGroupRequest virtualGroupRequest = new VirtualGroupRequest(envCrnForVirtualGroups, ldapConfig != null ? ldapConfig.getAdminGroup() : "");

            Builder builder = Builder.builder()
                    .withCloudPlatform(source.getCloudPlatform())
                    .withRdsConfigs(rdsConfigs)
                    .withHostgroupViews(hostgroupViews)
                    .withGateway(gateway, gatewaySignKey, exposedServiceCollector.getAllKnoxExposed())
                    .withBlueprintView(blueprintView)
                    .withFileSystemConfigurationView(fileSystemConfigurationView)
                    .withGeneralClusterConfigs(generalClusterConfigs)
                    .withLdapConfig(ldapConfig)
                    .withCustomInputs(source.getInputs())
                    .withKerberosConfig(getKerberosConfig(source, environment))
                    .withStackType(source.getType())
                    .withVirtualGroupView(virtualGroupRequest);
            decorateBuilderWithPlacement(source, builder);
            decorateBuilderWithAccountMapping(source, environment, credential, builder);
            decorateBuilderWithProductDetails(source, builder);
            decorateDatalakeView(source, builder);
            return builder.build();
        } catch (BlueprintProcessingException | IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private Credential getCredential(StackV4Request source, DetailedEnvironmentResponse environment) {
        return credentialConverter.convert(environment.getCredential());
    }

    private Blueprint getBlueprint(StackV4Request source, Workspace workspace) {
        return blueprintService.getByNameForWorkspace(source.getCluster().getBlueprintName(), workspace);
    }

    private Set<HostgroupView> getHostgroupViews(StackV4Request source) {
        Set<HostgroupView> hostgroupViews = new HashSet<>();
        for (InstanceGroupV4Request instanceGroup : source.getInstanceGroups()) {
            hostgroupViews.add(
                    new HostgroupView(
                            instanceGroup.getName(),
                            instanceGroup.getTemplate().getAttachedVolumes().stream().mapToInt(VolumeV4Request::getCount).sum(),
                            instanceGroup.getType(),
                            instanceGroup.getNodeCount()));
        }
        return hostgroupViews;
    }

    private Set<RDSConfig> getRdsConfigs(StackV4Request source, Workspace workspace) {
        return source.getCluster().getDatabases().stream()
                .map(d -> rdsConfigService.getByNameForWorkspace(d, workspace))
                .collect(Collectors.toSet());
    }

    private BaseFileSystemConfigurationsView getFileSystemConfigurationView(StackV4Request source, Json credentialAttributes)
            throws IOException {
        BaseFileSystemConfigurationsView fileSystemConfigurationView = null;
        if (isCloudStorageConfigured(source)) {
            FileSystem fileSystem = cloudStorageConverter.requestToFileSystem(source.getCluster().getCloudStorage());
            fileSystemConfigurationView = fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credentialAttributes,
                    cmCloudStorageConfigProvider.getConfigQueryEntries());
        }
        return fileSystemConfigurationView;
    }

    private LdapView getLdapConfig(StackV4Request source, DetailedEnvironmentResponse environment) {
        return LdapView.LdapViewBuilder.aLdapView()
                .withBindDn(String.format("uid=ldapbind-%s,cn=users,cn=accounts,dc=%s,dc=%s,dc=wl,dc=<account-name>,dc=site",
                        source.getName(),
                        environment.getName(),
                        source.getName()))
                .withBindPassword("dummy-password")
                .withDirectoryType(DirectoryType.LDAP)
                .withUserSearchBase(String.format("cn=users,cn=accounts,dc=%s,dc=%s,dc=wl,dc=<account-name>,dc=site",
                        environment.getName(),
                        source.getName()))
                .withUserNameAttribute("uid")
                .withUserObjectClass("posixAccount")
                .withGroupSearchBase(String.format("cn=groups,cn=accounts,dc=%s,dc=%s,dc=wl,dc=<account-name>,dc=site",
                        environment.getName(),
                        source.getName()))
                .withGroupMemberAttribute("cn")
                .withGroupObjectClass("posixGroup")
                .withGroupMemberAttribute("member")
                .withDomain(String.format("%s.%s.<account-name>.site",
                        environment.getName(),
                        source.getName()))
                .withProtocol("ldaps")
                .withAdminGroup("")
                .withUserGroup("ipausers")
                .withUserDnPattern(String.format("uid={0},cn=users,cn=accounts,dc=%s,dc=%s,dc=wl,dc=<account-name>,dc=site",
                        environment.getName(),
                        source.getName()))
                .withServerHost(String.format("ldap.%s.%s.wl.<account-name>.site",
                        environment.getName(),
                        source.getName()))
                .withConnectionURL(String.format("ldaps://ldap.%s.%s.wl.<account-name>.site:636",
                        environment.getName(),
                        source.getName()))
                .withServerPort(SERVER_PORT)
                .withCertificate(null)
                .build();
    }

    private KerberosConfig getKerberosConfig(StackV4Request source, DetailedEnvironmentResponse environment) {
        return KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withType(KerberosType.FREEIPA)
                .withPassword("dummy-password")
                .withUrl(String.format("kdc.%s.%s.wl.<account-name>.site",
                        environment.getName(),
                        source.getName()))
                .withAdminUrl(String.format("kerberos.%s.%s.wl.<account-name>.site",
                        environment.getName(),
                        source.getName()))
                .withRealm(String.format("%s.%s.WL.<ACCOUNT-NAME>.SITE",
                        environment.getName().toUpperCase(),
                        source.getName().toUpperCase()))
                .withTcpAllowed(false)
                .withPrincipal("kerberosbind-sdfsdfsf")
                .withVerifyKdcTrust(true)
                .withDomain(String.format("%s.%s.wl.<account-name>.site",
                        environment.getName(),
                        source.getName()))
                .withNameServers("10.112.23.17")
                .build();
    }

    private String getEnvironmentCrnForVirtualGroups(DetailedEnvironmentResponse environment) {
        String envCrnForVirtualGroups = environment.getCrn();
        if (StringUtils.isNoneEmpty(environment.getParentEnvironmentCrn())) {
            envCrnForVirtualGroups = environment.getParentEnvironmentCrn();
        }
        return envCrnForVirtualGroups;
    }

    private void decorateBuilderWithPlacement(StackV4Request source, Builder builder) {
        PlacementSettingsV4Request placementSettings = source.getPlacement();
        if (placementSettings != null) {
            String region = placementSettings.getRegion();
            builder.withPlacementView(new PlacementView(region, region));
        }
    }

    private void decorateBuilderWithAccountMapping(StackV4Request source, DetailedEnvironmentResponse environment, Credential credential, Builder builder) {
        if (source.getType() == StackType.DATALAKE) {
            AccountMappingBase accountMapping = isCloudStorageConfigured(source) ? source.getCluster().getCloudStorage().getAccountMapping() : null;
            if (accountMapping != null) {
                builder.withAccountMappingView(new AccountMappingView(accountMapping.getGroupMappings(), accountMapping.getUserMappings()));
            } else if (environment.getIdBrokerMappingSource() == IdBrokerMappingSource.MOCK) {
                Map<String, String> groupMappings;
                Map<String, String> userMappings;
                CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
                switch (source.getCloudPlatform()) {
                    case AWS:
                        groupMappings = awsMockAccountMappingService.getGroupMappings(source.getPlacement().getRegion(), cloudCredential,
                                environment.getAdminGroupName());
                        userMappings = awsMockAccountMappingService.getUserMappings(source.getPlacement().getRegion(), cloudCredential);
                        break;
                    case AZURE:
                        groupMappings = azureMockAccountMappingService.getGroupMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                cloudCredential,
                                environment.getAdminGroupName());
                        userMappings = azureMockAccountMappingService.getUserMappings(AzureMockAccountMappingService.MSI_RESOURCE_GROUP_NAME,
                                cloudCredential);
                        break;
                    case GCP:
                        groupMappings = gcpMockAccountMappingService.getGroupMappings(source.getPlacement().getRegion(),
                                cloudCredential,
                                environment.getAdminGroupName());
                        userMappings = gcpMockAccountMappingService.getUserMappings(source.getPlacement().getRegion(),
                                cloudCredential);
                        break;
                    default:
                        return;
                }
                builder.withAccountMappingView(new AccountMappingView(groupMappings, userMappings));
            }
        }
    }

    private void decorateBuilderWithProductDetails(StackV4Request source, TemplatePreparationObject.Builder builder) {
        // base image
        if (source.getCluster() != null && source.getCluster().getCm() != null && source.getCluster().getCm().getRepository() != null) {
            ClouderaManagerV4Request cm = source.getCluster().getCm();
            ClouderaManagerRepositoryV4Request repository = cm.getRepository();
            ClouderaManagerRepo cmRepo = new ClouderaManagerRepo()
                    .withBaseUrl(repository.getBaseUrl())
                    .withGpgKeyUrl(repository.getGpgKeyUrl())
                    .withVersion(repository.getVersion());
            List<ClouderaManagerProduct> products = null != cm.getProducts()
                    ? cm.getProducts().stream().map(StackV4RequestToTemplatePreparationObjectConverter::convertProduct).collect(toList())
                    : new ArrayList<>();
            builder.withProductDetails(cmRepo, products);
            // prewarm image
        }
        // TODO: implement else {} branch for prewarm images
    }

    private void decorateDatalakeView(StackV4Request source, TemplatePreparationObject.Builder builder) {
        DatalakeView datalakeView = null;
        if (StringUtils.isNotEmpty(source.getEnvironmentCrn()) && StackType.WORKLOAD.equals(source.getType())) {
            List<SdxClusterResponse> datalakes = sdxClientService.getByEnvironmentCrn(source.getEnvironmentCrn());
            if (!datalakes.isEmpty()) {
                datalakeView = new DatalakeView(datalakes.get(0).getRangerRazEnabled());
            }
        }
        builder.withDataLakeView(datalakeView);
    }

    private static ClouderaManagerProduct convertProduct(ClouderaManagerProductV4Request productRequest) {
        return new ClouderaManagerProduct()
                .withName(productRequest.getName())
                .withVersion(productRequest.getVersion())
                .withParcel(productRequest.getParcel())
                .withCsd(productRequest.getCsd());
    }

    private boolean isCloudStorageConfigured(StackV4Request source) {
        return cloudStorageValidationUtil.isCloudStorageConfigured(source.getCluster().getCloudStorage());
    }

}

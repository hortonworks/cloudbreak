package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.STARTED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.job.StackJobAdapter;
import com.sequenceiq.cloudbreak.job.archiver.instancemetadata.ArchiveInstanceMetaDataJobService;
import com.sequenceiq.cloudbreak.job.dynamicentitlement.DynamicEntitlementRefreshJobService;
import com.sequenceiq.cloudbreak.job.provider.ProviderSyncJobService;
import com.sequenceiq.cloudbreak.job.stackpatcher.ExistingStackPatcherJobService;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AutoConfigureClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AutoConfigureClusterManagerSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ConfigurePolicyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ConfigurePolicySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerConfigureKerberosSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerRefreshParcelRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerRefreshParcelSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerSupportTagsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerSupportTagsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostServiceDeploymentRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostServiceDeploymentRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.FinalizeClusterInstallSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareExtendedTemplateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareExtendedTemplateSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.StartClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.StartClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SuppressClusterWarningsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SuppressClusterWarningsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ValidateClusterLicenceRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ValidateClusterLicenceSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForClusterManagerSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapFreeIPAEndpointSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapPublicEndpointSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterCreationFailedRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoveryRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoverySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ValidateCloudStorageRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupAdEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionEvent;
import com.sequenceiq.cloudbreak.service.freeipa.InstanceMetadataProcessor;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.job.StructuredSynchronizerJobAdapter;
import com.sequenceiq.cloudbreak.structuredevent.job.StructuredSynchronizerJobService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.PayloadConverter;

@Configuration
public class ClusterCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationActions.class);

    @Inject
    private ClusterCreationService clusterCreationService;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private ArchiveInstanceMetaDataJobService aimJobService;

    @Inject
    private StructuredSynchronizerJobService syncJobService;

    @Inject
    private InstanceMetadataProcessor instanceMetadataProcessor;

    @Inject
    private ExistingStackPatcherJobService existingStackPatcherJobService;

    @Inject
    private DynamicEntitlementRefreshJobService dynamicEntitlementRefreshJobService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MeteringService meteringService;

    @Inject
    private ProviderSyncJobService providerSyncJobService;

    @Inject
    private StackUtil stackUtil;

    @Bean(name = "CLUSTER_PROXY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyRegistrationAction() {
        return new AbstractStackCreationAction<>(ProvisionEvent.class) {

            @Inject
            private ClusterProxyEnablementService clusterProxyEnablementService;

            @Override
            protected void prepareExecution(ProvisionEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(PROVISION_TYPE, payload.getProvisionType());
            }

            @Override
            protected void doExecute(StackCreationContext context, ProvisionEvent payload, Map<Object, Object> variables) {
                if (clusterProxyEnablementService.isClusterProxyApplicable(context.getCloudPlatform())) {
                    clusterCreationService.registeringToClusterProxy(context.getStackId());
                    sendEvent(context);
                } else {
                    ClusterProxyRegistrationSuccess clusterProxyRegistrationSuccess = new ClusterProxyRegistrationSuccess(payload.getResourceId());
                    sendEvent(context, clusterProxyRegistrationSuccess);
                }
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ClusterProxyRegistrationRequest(context.getStackId(), context.getCloudPlatform());
            }
        };
    }

    @Bean(name = "BOOTSTRAPPING_MACHINES_STATE")
    public Action<?, ?> bootstrappingMachinesAction() {
        return new AbstractStackCreationAction<>(ClusterProxyRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, ClusterProxyRegistrationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.bootstrappingMachines(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new BootstrapMachinesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "COLLECTING_HOST_METADATA_STATE")
    public Action<?, ?> collectingHostMetadataAction() {
        return new AbstractStackCreationAction<>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                // UNUSED STEP
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new HostMetadataSetupSuccess(context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_CLOUD_STORAGE_STATE")
    public Action<?, ?> validateCloudStorageAction() {
        return new AbstractStackCreationAction<>(HostMetadataSetupSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.validatingCloudStorageOnVm(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ValidateCloudStorageRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "SETUP_RECOVERY_STATE")
    public Action<?, ?> setupRecoveryAction() {
        return new AbstractStackCreationAction<>(HostMetadataSetupSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<HostMetadataSetupSuccess>> payloadConverters) {
                super.initPayloadConverterMap(payloadConverters);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new SetupRecoveryRequest(context.getStackId(), context.getProvisionType());
            }
        };
    }

    @Bean(name = "CLEANUP_FREEIPA_STATE")
    public Action<?, ?> cleanupFreeIpaAction() {
        return new AbstractStackCreationAction<>(SetupRecoverySuccess.class) {

            @Inject
            private InstanceMetaDataService instanceMetaDataService;

            @Inject
            private KerberosConfigService kerberosConfigService;

            @Inject
            private KerberosDetailService kerberosDetailService;

            @Override
            protected void doExecute(StackCreationContext context, SetupRecoverySuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                if (isAdInUse(context.getStack())) {
                    StackDto stack = stackDtoService.getById(context.getStackId());
                    Set<String> allNodeHostnames =
                            stackUtil.collectNodes(stack).stream().map(Node::getHostname).collect(Collectors.toSet());
                    return new CleanupAdEvent(context.getStackId(), allNodeHostnames, Collections.emptySet());
                } else {
                    List<InstanceMetadataView> instanceMetaData = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(context.getStackId());
                    Set<String> hostNames = instanceMetadataProcessor.extractFqdn(instanceMetaData);
                    Set<String> ips = instanceMetadataProcessor.extractIps(instanceMetaData);
                    return new CleanupFreeIpaEvent(context.getStackId(), hostNames, ips, false);
                }
            }

            private boolean isAdInUse(StackView stack) {
                Optional<KerberosConfig> kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName());
                if (kerberosConfig.isPresent()) {
                    return kerberosDetailService.isAdJoinable(kerberosConfig.get());
                } else {
                    LOGGER.info("Kerberos config not found for stack: {}", stack.getId());
                    return false;
                }
            }
        };
    }

    @Bean(name = "BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE")
    public Action<?, ?> bootStrappingPublicEndpointAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, StackEvent payload, Map<Object, Object> variables) {
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                clusterCreationService.bootstrapPublicEndpoints(stack);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new BootstrapPublicEndpointSuccess(context.getStackId());
            }
        };
    }

    @Bean(name = "BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE")
    public Action<?, ?> bootStrappingPrivateEndpointAction() {
        return new AbstractStackCreationAction<>(BootstrapPublicEndpointSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, BootstrapPublicEndpointSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.bootstrapPrivateEndpoints(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new BootstrapFreeIPAEndpointSuccess(context.getStackId());
            }
        };
    }

    @Bean(name = "UPLOAD_RECIPES_STATE")
    public Action<?, ?> uploadRecipesAction() {
        return new AbstractClusterCreationAction<>(BootstrapFreeIPAEndpointSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, BootstrapFreeIPAEndpointSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new UploadRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_KEYTABS_STATE")
    public Action<?, ?> configureKeytabsAction() {
        return new AbstractClusterCreationAction<>(UploadRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, UploadRecipesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new KeytabConfigurationRequest(context.getStackId(), Boolean.FALSE);
            }
        };
    }

    @Bean(name = "STARTING_CLUSTER_MANAGER_SERVICES_STATE")
    public Action<?, ?> startingAmbariServicesAction() {
        return new AbstractClusterCreationAction<>(KeytabConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, KeytabConfigurationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.startingClusterServices(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new StartAmbariServicesRequest(context.getStackId(), true, true);
            }
        };
    }

    @Bean(name = "PREFLIGHT_CHECK_STATE")
    public Action<?, ?> preFlightCheckAction() {
        return new AbstractClusterCreationAction<>(StartClusterManagerServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, StartClusterManagerServicesSuccess payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting pre-flight checks for stack with id: {}", context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new PreFlightCheckRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "STARTING_CLUSTER_MANAGER_STATE")
    public Action<?, ?> startingAmbariAction() {
        return new AbstractClusterCreationAction<>(PreFlightCheckSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, PreFlightCheckSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.startingClusterManager(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new StartClusterRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_LDAP_SSO_STATE")
    public Action<?, ?> configureLdapSSOAction() {
        return new AbstractClusterCreationAction<>(StartClusterSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, StartClusterSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new LdapSSOConfigurationRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "WAIT_FOR_CLUSTER_MANAGER_STATE")
    public Action<?, ?> waitForClusterManagerAction() {
        return new AbstractClusterCreationAction<>(LdapSSOConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, LdapSSOConfigurationSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new WaitForClusterManagerRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_STATE")
    public Action<?, ?> executePostClusterManagerStartRecipesAction() {
        return new AbstractClusterCreationAction<>(WaitForClusterManagerSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, WaitForClusterManagerSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ExecutePostClusterManagerStartRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "PREPARE_PROXY_CONFIG_STATE")
    public Action<?, ?> prepareProxyConfigAction() {
        return new AbstractClusterCreationAction<>(ExecutePostClusterManagerStartRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ExecutePostClusterManagerStartRecipesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ClusterManagerPrepareProxyConfigRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "SETUP_MONITORING_STATE")
    public Action<?, ?> setupMonitoringAction() {
        return new AbstractClusterCreationAction<>(ClusterManagerPrepareProxyConfigSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ClusterManagerPrepareProxyConfigSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ClusterManagerSetupMonitoringRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "PREPARE_EXTENDED_TEMPLATE_STATE")
    public Action<?, ?> prepareExtendedTemplateAction() {
        return new AbstractClusterCreationAction<>(ClusterManagerSetupMonitoringSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ClusterManagerSetupMonitoringSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new PrepareExtendedTemplateRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "VALIDATE_LICENCE_STATE")
    public Action<?, ?> validateLicenceAction() {
        return new AbstractClusterCreationAction<>(PrepareExtendedTemplateSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, PrepareExtendedTemplateSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ValidateClusterLicenceRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_MANAGEMENT_SERVICES_STATE")
    public Action<?, ?> configureManagementServicesAction() {
        return new AbstractClusterCreationAction<>(ValidateClusterLicenceSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ValidateClusterLicenceSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ConfigureClusterManagerManagementServicesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_SUPPORT_TAGS_STATE")
    public Action<?, ?> configureSupportTagsAction() {
        return new AbstractClusterCreationAction<>(ConfigureClusterManagerManagementServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context,
                    ConfigureClusterManagerManagementServicesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ConfigureClusterManagerSupportTagsRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPDATE_CONFIG_STATE")
    public Action<?, ?> updateConfigAction() {
        return new AbstractClusterCreationAction<>(ConfigureClusterManagerSupportTagsSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ConfigureClusterManagerSupportTagsSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new UpdateClusterConfigRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "REFRESH_PARCEL_REPOS_STATE")
    public Action<?, ?> refreshParcelReposAction() {
        return new AbstractClusterCreationAction<>(UpdateClusterConfigSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, UpdateClusterConfigSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ClusterManagerRefreshParcelRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPDATING_CONFIG_POLICIES_STATE")
    public Action<?, ?> configurePolicyAction() {
        return new AbstractClusterCreationAction<>(ClusterManagerRefreshParcelSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ClusterManagerRefreshParcelSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ConfigurePolicyRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "INSTALLING_CLUSTER_STATE")
    public Action<?, ?> installingClusterAction() {
        return new AbstractClusterCreationAction<>(ConfigurePolicySuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ConfigurePolicySuccess payload, Map<Object, Object> variables) {
                clusterCreationService.installingCluster(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new InstallClusterRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "AUTOCONFIGURE_CLUSTER_MANAGER_STATE")
    public Action<?, ?> autoConfiguerClusterManagerAction() {
        return new AbstractClusterCreationAction<>(InstallClusterSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, InstallClusterSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new AutoConfigureClusterManagerRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "START_MANAGEMENT_SERVICES_STATE")
    public Action<?, ?> startManagementServicesAction() {
        return new AbstractClusterCreationAction<>(AutoConfigureClusterManagerSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, AutoConfigureClusterManagerSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new StartClusterManagerManagementServicesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "SUPPRESS_WARNINGS_STATE")
    public Action<?, ?> suppressWarningsAction() {
        return new AbstractClusterCreationAction<>(StartClusterManagerManagementServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, StartClusterManagerManagementServicesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new SuppressClusterWarningsRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_KERBEROS_STATE")
    public Action<?, ?> configureKerberosAction() {
        return new AbstractClusterCreationAction<>(SuppressClusterWarningsSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, SuppressClusterWarningsSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ClusterManagerConfigureKerberosRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "EXECUTE_POST_INSTALL_RECIPES_STATE")
    public Action<?, ?> executePostServiceDeploymentRecipesAction() {
        return new AbstractClusterCreationAction<>(ClusterManagerConfigureKerberosSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ClusterManagerConfigureKerberosSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new ExecutePostServiceDeploymentRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "PREPARE_DATALAKE_RESOURCE_STATE")
    public Action<?, ?> prepareDatalakeResourceAction() {
        return new AbstractClusterCreationAction<>(ExecutePostServiceDeploymentRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ExecutePostServiceDeploymentRecipesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new PrepareDatalakeResourceRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "FINALIZE_CLUSTER_INSTALL_STATE")
    public Action<?, ?> finalizeClusterInstallAction() {
        return new AbstractClusterCreationAction<>(PrepareDatalakeResourceSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, PrepareDatalakeResourceSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new FinalizeClusterInstallRequest(context.getStackId(), context.getProvisionType());
            }
        };
    }

    @Bean(name = "CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyGatewayRegistrationAction() {
        return new AbstractStackCreationAction<>(FinalizeClusterInstallSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, FinalizeClusterInstallSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ClusterProxyGatewayRegistrationRequest(context.getStackId(), context.getCloudPlatform());
            }
        };
    }

    @Bean(name = "CLUSTER_CREATION_FINISHED_STATE")
    public Action<?, ?> clusterCreationFinishedAction() {
        return new AbstractClusterCreationAction<>(ClusterProxyGatewayRegistrationSuccess.class) {
            @Override
            protected void doExecute(ClusterCreationViewContext context, ClusterProxyGatewayRegistrationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.clusterInstallationFinished(context.getStackId(), context.getProvisionType());
                jobService.schedule(context.getStackId(), StackJobAdapter.class);
                syncJobService.schedule(context.getStackId(), StructuredSynchronizerJobAdapter.class);
                aimJobService.schedule(context.getStackId());
                dynamicEntitlementRefreshJobService.schedule(context.getStackId());
                providerSyncJobService.schedule(context.getStackId());
                if (CloudPlatform.MOCK.equalsIgnoreCase(context.getStack().getCloudPlatform())) {
                    existingStackPatcherJobService.schedule(context.getStackId(), StackPatchType.MOCK);
                }
                meteringService.sendMeteringStatusChangeEventForStack(context.getStackId(), STARTED);
                meteringService.scheduleSync(context.getStackId());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_CREATION_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterCreationViewContext context) {
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_CREATION_FAILED_STATE")
    public Action<?, ?> clusterCreationFailedAction() {
        return new AbstractStackFailureAction<ClusterCreationState, ClusterCreationEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Cluster creation failed with exception", payload.getException());
                clusterCreationService.handleClusterCreationFailure(context.getStack(), payload.getException(), context.getProvisionType());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_CREATION_FAILED, context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new ClusterCreationFailedRequest(context.getStackId());
            }
        };
    }
}
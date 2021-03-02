package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_BUILDING;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.job.StackJobAdapter;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostClusterManagerStartRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostInstallRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ExecutePostInstallRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.HandleClusterCreationSuccessRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.HandleClusterCreationSuccessSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareDatalakeResourceSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareExtendedTemplateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareExtendedTemplateSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.PrepareProxyConfigSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SetupMonitoringRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.SetupMonitoringSuccess;
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
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.service.freeipa.InstanceMetadataProcessor;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.structuredevent.job.StructuredSynchronizerJobService;
import com.sequenceiq.cloudbreak.structuredevent.job.StructuredSynchronizerJobAdapter;

@Configuration
public class ClusterCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationActions.class);

    @Inject
    private ClusterCreationService clusterCreationService;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private StructuredSynchronizerJobService syncJobService;

    @Inject
    private InstanceMetadataProcessor instanceMetadataProcessor;

    @Bean(name = "CLUSTER_PROXY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyRegistrationAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Inject
            private ClusterProxyEnablementService clusterProxyEnablementService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                if (clusterProxyEnablementService.isClusterProxyApplicable(context.getStack().cloudPlatform())) {
                    clusterCreationService.registeringToClusterProxy(context.getStack());
                    sendEvent(context);
                } else {
                    ClusterProxyRegistrationSuccess clusterProxyRegistrationSuccess = new ClusterProxyRegistrationSuccess(payload.getResourceId());
                    sendEvent(context, clusterProxyRegistrationSuccess);
                }
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyRegistrationRequest(context.getStack().getId(), context.getStack().cloudPlatform());
            }
        };
    }

    @Bean(name = "BOOTSTRAPPING_MACHINES_STATE")
    public Action<?, ?> bootstrappingMachinesAction() {
        return new AbstractStackCreationAction<>(ClusterProxyRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, ClusterProxyRegistrationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.bootstrappingMachines(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new BootstrapMachinesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "COLLECTING_HOST_METADATA_STATE")
    public Action<?, ?> collectingHostMetadataAction() {
        return new AbstractStackCreationAction<>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.collectingHostMetadata(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new HostMetadataSetupRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLEANUP_FREEIPA_STATE")
    public Action<?, ?> cleanupFreeIpaAction() {
        return new AbstractStackCreationAction<>(HostMetadataSetupSuccess.class) {

            @Inject
            private InstanceMetaDataService instanceMetaDataService;

            @Override
            protected void doExecute(StackContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.findNotTerminatedForStack(context.getStack().getId());
                Set<String> hostNames = instanceMetadataProcessor.extractFqdn(instanceMetaData);
                Set<String> ips = instanceMetadataProcessor.extractIps(instanceMetaData);
                return new CleanupFreeIpaEvent(context.getStack().getId(), hostNames, ips, false);
            }
        };
    }

    @Bean(name = "BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE")
    public Action<?, ?> bootStrappingPublicEndpointAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                clusterCreationService.bootstrapPublicEndpoints(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new BootstrapPublicEndpointSuccess(context.getStack().getId());
            }
        };
    }

    @Bean(name = "BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE")
    public Action<?, ?> bootStrappingPrivateEndpointAction() {
        return new AbstractStackCreationAction<>(BootstrapPublicEndpointSuccess.class) {
            @Override
            protected void doExecute(StackContext context, BootstrapPublicEndpointSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.bootstrapPrivateEndpoints(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new BootstrapFreeIPAEndpointSuccess(context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPLOAD_RECIPES_STATE")
    public Action<?, ?> uploadRecipesAction() {
        return new AbstractClusterAction<>(BootstrapFreeIPAEndpointSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, BootstrapFreeIPAEndpointSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UploadRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_KEYTABS_STATE")
    public Action<?, ?> configureKeytabsAction() {
        return new AbstractClusterAction<>(UploadRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UploadRecipesSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new KeytabConfigurationRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "STARTING_CLUSTER_MANAGER_SERVICES_STATE")
    public Action<?, ?> startingAmbariServicesAction() {
        return new AbstractClusterAction<>(KeytabConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, KeytabConfigurationSuccess payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.startingClusterServices(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StartAmbariServicesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "STARTING_CLUSTER_MANAGER_STATE")
    public Action<?, ?> startingAmbariAction() {
        return new AbstractClusterAction<>(StartClusterManagerServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StartClusterManagerServicesSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.startingClusterManager(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StartClusterRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CONFIGURE_LDAP_SSO_STATE")
    public Action<?, ?> configureLdapSSOAction() {
        return new AbstractClusterAction<>(StartClusterSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StartClusterSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new LdapSSOConfigurationRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "WAIT_FOR_CLUSTER_MANAGER_STATE")
    public Action<?, ?> waitForClusterManagerAction() {
        return new AbstractClusterAction<>(LdapSSOConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, LdapSSOConfigurationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.waitClusterManagerStart(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new WaitForClusterManagerRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_STATE")
    public Action<?, ?> executePostClusterManagerStartRecipesAction() {
        return new AbstractClusterAction<>(WaitForClusterManagerSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, WaitForClusterManagerSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ExecutePostClusterManagerStartRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "PREPARE_PROXY_CONFIG_STATE")
    public Action<?, ?> prepareProxyConfigAction() {
        return new AbstractClusterAction<>(ExecutePostClusterManagerStartRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ExecutePostClusterManagerStartRecipesSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new PrepareProxyConfigRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "SETUP_MONITORING_STATE")
    public Action<?, ?> setupMonitoringAction() {
        return new AbstractClusterAction<>(PrepareProxyConfigSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, PrepareProxyConfigSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new SetupMonitoringRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "PREPARE_EXTENDED_TEMPLATE_STATE")
    public Action<?, ?> prepareExtendedTemplateAction() {
        return new AbstractClusterAction<>(SetupMonitoringSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, SetupMonitoringSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new PrepareExtendedTemplateRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "INSTALLING_CLUSTER_STATE")
    public Action<?, ?> installingClusterAction() {
        return new AbstractClusterAction<>(PrepareExtendedTemplateSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, PrepareExtendedTemplateSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new InstallClusterRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "EXECUTE_POST_INSTALL_RECIPES_STATE")
    public Action<?, ?> executePostInstallRecipesAction() {
        return new AbstractClusterAction<>(InstallClusterSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, InstallClusterSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new ExecutePostInstallRecipesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "PREPARE_DATALAKE_RESOURCE_STATE")
    public Action<?, ?> prepareDatalakeResourceAction() {
        return new AbstractClusterAction<>(ExecutePostInstallRecipesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ExecutePostInstallRecipesSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new PrepareDatalakeResourceRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "HANDLE_CLUSTER_CREATION_SUCCESS_STATE")
    public Action<?, ?> handleClusterCreationSuccessAction() {
        return new AbstractClusterAction<>(PrepareDatalakeResourceSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, PrepareDatalakeResourceSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.updateCluster(context.getStack(), "Building the cluster", CLUSTER_BUILDING);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new HandleClusterCreationSuccessRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyGatewayRegistrationAction() {
        return new AbstractStackCreationAction<>(PrepareDatalakeResourceSuccess.class) {
            @Override
            protected void doExecute(StackContext context, PrepareDatalakeResourceSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.registeringGatewayToClusterProxy(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyGatewayRegistrationRequest(context.getStack().getId(), context.getStack().cloudPlatform());
            }
        };
    }

    @Bean(name = "CLUSTER_CREATION_FINISHED_STATE")
    public Action<?, ?> clusterCreationFinishedAction() {
        return new AbstractClusterAction<>(ClusterProxyGatewayRegistrationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterProxyGatewayRegistrationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.clusterInstallationFinished(context.getStack());
                jobService.schedule(context.getStackId(), StackJobAdapter.class);
                syncJobService.schedule(context.getStackId(), StructuredSynchronizerJobAdapter.class);
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_CREATION_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
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
                clusterCreationService.handleClusterCreationFailure(context.getStackView(), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_CREATION_FAILED, context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.MountDisksRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.MountDisksSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;

@Configuration
public class ClusterCreationActions {
    @Inject
    private ClusterCreationService clusterCreationService;

    @Bean(name = "CLUSTER_PROXY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyRegistrationAction() {
        return new AbstractStackCreationAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                clusterCreationService.registeringToClusterProxy(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyRegistrationRequest(context.getStack().getId());
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

    @Bean(name = "MOUNT_DISKS_STATE")
    public Action<?, ?> collectingMountDisksAction() {
        return new AbstractStackCreationAction<>(HostMetadataSetupSuccess.class) {
            @Override
            protected void doExecute(StackContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.generateCertAndDnsEntryForGateway(context.getStack());
                clusterCreationService.mountDisks(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new MountDisksRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPLOAD_RECIPES_STATE")
    public Action<?, ?> uploadRecipesAction() {
        return new AbstractClusterAction<>(MountDisksSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, MountDisksSuccess payload, Map<Object, Object> variables) {
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

    @Bean(name = "STARTING_AMBARI_SERVICES_STATE")
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

    @Bean(name = "STARTING_AMBARI_STATE")
    public Action<?, ?> startingAmbariAction() {
        return new AbstractClusterAction<>(StartAmbariServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StartAmbariServicesSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.startingAmbari(context.getStackId());
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

    @Bean(name = "INSTALLING_CLUSTER_STATE")
    public Action<?, ?> installingClusterAction() {
        return new AbstractClusterAction<>(LdapSSOConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, LdapSSOConfigurationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.installingCluster(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new InstallClusterRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CLUSTER_CREATION_FINISHED_STATE")
    public Action<?, ?> clusterCreationFinishedAction() {
        return new AbstractClusterAction<>(ClusterProxyGatewayRegistrationSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterProxyGatewayRegistrationSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.clusterInstallationFinished(context.getStack());
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
                clusterCreationService.handleClusterCreationFailure(context.getStackView(), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_CREATION_FAILED, context.getStackView());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE")
    public Action<?, ?> clusterProxyGatewayRegistrationAction() {
        return new AbstractStackCreationAction<>(InstallClusterSuccess.class) {
            @Override
            protected void doExecute(StackContext context, InstallClusterSuccess payload, Map<Object, Object> variables) {
                clusterCreationService.registeringGatewayToClusterProxy(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyGatewayRegistrationRequest(context.getStack().getId());
            }
        };
    }
}

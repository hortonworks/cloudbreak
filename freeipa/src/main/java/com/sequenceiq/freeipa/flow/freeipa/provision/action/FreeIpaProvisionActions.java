package com.sequenceiq.freeipa.flow.freeipa.provision.action;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionState;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.cloudstorage.ValidateCloudStorageSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.orchestrator.OrchestratorConfigSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaSuccess;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesSuccess;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.action.AbstractStackProvisionAction;
import com.sequenceiq.freeipa.metrics.FreeIpaMetricService;
import com.sequenceiq.freeipa.metrics.MetricType;
import com.sequenceiq.freeipa.service.config.AbstractConfigRegister;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@Configuration
public class FreeIpaProvisionActions {

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FreeIpaMetricService metricService;

    @Bean(name = "BOOTSTRAPPING_MACHINES_STATE")
    public Action<?, ?> bootstrappingMachinesAction() {
        return new AbstractStackProvisionAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.BOOTSTRAPPING_MACHINES, "Bootstrapping machines");
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
        return new AbstractStackProvisionAction<>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.COLLECTING_HOST_METADATA, "Collecting host metadata");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new HostMetadataSetupRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "ORCHESTRATOR_CONFIG_STATE")
    public Action<?, ?> orchestratorConfig() {
        return new AbstractStackProvisionAction<>(HostMetadataSetupSuccess.class) {

            @Override
            protected void doExecute(StackContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.CONFIGURING_ORCHESTRATOR, "Configuring the orchestrator");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new OrchestratorConfigRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "VALIDATING_CLOUD_STORAGE_STATE")
    public Action<?, ?> validateFreeIpaCloudStorage() {
        return new AbstractStackProvisionAction<>(OrchestratorConfigSuccess.class) {

            @Override
            protected void doExecute(StackContext context, OrchestratorConfigSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.VALIDATING_CLOUD_STORAGE, "Validating cloud storage");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ValidateCloudStorageRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "FREEIPA_INSTALL_STATE")
    public Action<?, ?> installFreeIpa() {
        return new AbstractStackProvisionAction<>(ValidateCloudStorageSuccess.class) {

            @Override
            protected void doExecute(StackContext context, ValidateCloudStorageSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.STARTING_FREEIPA_SERVICES, "Starting FreeIPA services");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new InstallFreeIpaServicesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTERPROXY_UPDATE_REGISTRATION_STATE")
    public Action<?, ?> updateClusterProxyRegistrationAction() {
        return new AbstractStackProvisionAction<>(InstallFreeIpaServicesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, InstallFreeIpaServicesSuccess payload, Map<Object, Object> variables) throws Exception {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.UPDATE_CLUSTER_PROXY_REGISTRATION,
                        "Updating cluster proxy registration.");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new ClusterProxyUpdateRegistrationRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "FREEIPA_POST_INSTALL_STATE")
    public Action<?, ?> postInstallFreeIpa() {
        return new AbstractStackProvisionAction<>(ClusterProxyUpdateRegistrationSuccess.class) {

            @Override
            protected void doExecute(StackContext context, ClusterProxyUpdateRegistrationSuccess payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.POSTINSTALL_FREEIPA_CONFIGURATION,
                        "Performing FreeIPA post-install configuration");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new PostInstallFreeIpaRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "FREEIPA_PROVISION_FINISHED_STATE")
    public Action<?, ?> provisionFinished() {
        return new AbstractStackProvisionAction<>(PostInstallFreeIpaSuccess.class) {

            @Inject
            private Set<AbstractConfigRegister> configRegisters;

            @Inject
            private FreeipaJobService freeipaJobService;

            @Override
            protected void doExecute(StackContext context, PostInstallFreeIpaSuccess payload, Map<Object, Object> variables) {
                configRegisters.forEach(configProvider -> configProvider.register(context.getStack().getId()));
                metricService.incrementMetricCounter(MetricType.FREEIPA_CREATION_FINISHED, context.getStack());
                freeipaJobService.schedule(context.getStack().getId());
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.PROVISIONED, "FreeIPA installation finished");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(FreeIpaProvisionEvent.FREEIPA_PROVISION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "FREEIPA_PROVISION_FAILED_STATE")
    public Action<?, ?> handleProvisionFailure() {
        return new AbstractStackFailureAction<FreeIpaProvisionState, FreeIpaProvisionEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.PROVISION_FAILED, errorReason);
                metricService.incrementMetricCounter(MetricType.FREEIPA_CREATION_FAILED, context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(FreeIpaProvisionEvent.FREEIPA_PROVISION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.SCALE_UP;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRepairSingleMasterStartResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRestartAllRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStartServerAndAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerEnsureComponentsAreStoppedResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerRestartAllResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStopComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.EnsureClusterComponentsAreStoppedRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopClusterComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataResult;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class ClusterUpscaleActions {
    @Inject
    private ClusterUpscaleFlowService clusterUpscaleFlowService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private MeteringService meteringService;

    @Bean(name = "UPLOAD_UPSCALE_RECIPES_STATE")
    public Action<?, ?> uploadUpscaleRecipesAction() {
        return new AbstractClusterUpscaleAction<>(ClusterScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOST_GROUP_WITH_ADJUSTMENT, payload.getHostGroupsWithAdjustment());
                variables.put(SINGLE_PRIMARY_GATEWAY, payload.isSinglePrimaryGateway());
                variables.put(KERBEROS_SECURED, payload.isKerberosSecured());
                variables.put(SINGLE_NODE_CLUSTER, payload.isSingleNodeCluster());
                variables.put(CLUSTER_MANAGER_TYPE, payload.getClusterManagerType());
                variables.put(RESTART_SERVICES, payload.isRestartServices());
                variables.put(HOST_NAMES_BY_HOST_GROUP, payload.getHostGroupsWithHostNames());
                variables.put(ROLLING_RESTART_ENABLED, payload.isRollingRestartEnabled());
                if (payload.isSinglePrimaryGateway()) {
                    variables.put(HOST_NAME, getMasterHostname(payload));
                }
            }

            private String getMasterHostname(ClusterScaleTriggerEvent payload) {
                StackDto stackDto = stackDtoService.getById(payload.getResourceId());
                return gatewayConfigService.getPrimaryGatewayConfig(stackDto).getHostname();
            }

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.upscalingClusterManager(context.getStackId(), payload.getHostGroups());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UploadUpscaleRecipesRequest(context.getStackId(), context.getHostGroups());
            }
        };
    }

    @Bean(name = "UPSCALE_PREFLIGHT_CHECK_STATE")
    public Action<?, ?> preFlightCheckAction() {
        return new AbstractClusterUpscaleAction<>(UploadUpscaleRecipesResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UploadUpscaleRecipesResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.runPreflightCheck(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new PreFlightCheckRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "RECONFIGURE_KEYTABS_STATE")
    public Action<?, ?> configureKeytabsAction() {
        return new AbstractClusterUpscaleAction<>(PreFlightCheckSuccess.class) {
            @Override
            protected void doExecute(ClusterUpscaleContext context, PreFlightCheckSuccess payload, Map<Object, Object> variables) {
                if (context.isSinglePrimaryGateway() && ClusterManagerType.CLOUDERA_MANAGER.equals(context.getClusterManagerType())) {
                    KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(context.getStackId(), context.isRepair());
                    sendEvent(context, keytabConfigurationRequest.selector(), keytabConfigurationRequest);
                } else {
                    KeytabConfigurationSuccess keytabConfigurationSuccess = new KeytabConfigurationSuccess(context.getStackId());
                    sendEvent(context, keytabConfigurationSuccess.selector(), keytabConfigurationSuccess);
                }
            }
        };
    }

    @Bean(name = "CHECK_HOST_METADATA_STATE")
    public Action<?, ?> checkHostMetadataAction() {
        return new AbstractClusterUpscaleAction<>(KeytabConfigurationSuccess.class) {
            @Override
            protected void doExecute(ClusterUpscaleContext context, KeytabConfigurationSuccess payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscaleCheckHostMetadataRequest(context.getStackId(), context.getHostGroups(),
                        context.getPrimaryGatewayHostName(), context.isSinglePrimaryGateway());
            }
        };
    }

    @Bean(name = "UPSCALING_CLUSTER_MANAGER_STATE")
    public Action<?, ?> upscalingClusterManagerAction() {
        return new AbstractClusterUpscaleAction<>(UpscaleCheckHostMetadataResult.class) {
            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscaleCheckHostMetadataResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscaleClusterManagerRequest(context.getStackId(), context.getHostGroupWithAdjustment(),
                        context.isSinglePrimaryGateway(), context.isRepair());
            }

        };
    }

    @Bean(name = "UPSCALING_CLUSTER_MANAGER_FINISHED_STATE")
    public Action<?, ?> upscalingClusterManagerFinishedAction() {
        return new AbstractClusterUpscaleAction<>(UpscaleClusterManagerResult.class) {
            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscaleClusterManagerResult payload, Map<Object, Object> variables) {
                if (context.isSinglePrimaryGateway() && ClusterManagerType.AMBARI.equals(context.getClusterManagerType())) {
                    clusterUpscaleFlowService.clusterManagerRepairSingleMasterStarted(context.getStackId());
                    AmbariRepairSingleMasterStartResult result =
                            new AmbariRepairSingleMasterStartResult(context.getStackId(), context.getHostGroups());
                    sendEvent(context, result.selector(), result);
                } else {
                    Map<String, Collection<String>> hostGroupsWithHostNames = getHostNamesByHostGroup(variables);
                    UpscaleClusterRequest request = new UpscaleClusterRequest(context.getStackId(), context.getHostGroups(),
                            context.isRepair(), context.isRestartServices(), hostGroupsWithHostNames, context.getHostGroupWithAdjustment(),
                            context.isSinglePrimaryGateway(), isRollingRestartEnabled(variables));
                    sendEvent(context, request.selector(), request);
                }
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_STATE")
    public Action<?, ?> clusterManagerGatherInstalledComponentsAction() {
        return new AbstractClusterUpscaleAction<>(AmbariRepairSingleMasterStartResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, AmbariRepairSingleMasterStartResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new AmbariGatherInstalledComponentsRequest(context.getStackId(), context.getHostGroups(), context.getPrimaryGatewayHostName());
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_STOP_COMPONENTS_STATE")
    public Action<?, ?> clusterManagerStopComponentsAction() {
        return new AbstractClusterUpscaleAction<>(AmbariGatherInstalledComponentsResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, AmbariGatherInstalledComponentsResult payload, Map<Object, Object> variables) {
                Map<String, String> components = payload.getFoundInstalledComponents();
                variables.put(INSTALLED_COMPONENTS, components);
                ClusterManagerStopComponentsRequest request =
                        new ClusterManagerStopComponentsRequest(context.getStackId(), context.getHostGroups(),
                                context.getPrimaryGatewayHostName(), components);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_STOP_SERVER_AGENT_STATE")
    public Action<?, ?> clusterManagerStopServerAndAgentAction() {
        return new AbstractClusterUpscaleAction<>(StopClusterComponentsResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, StopClusterComponentsResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.stopClusterManagementServer(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new AmbariStopServerAndAgentRequest(context.getStackId(), context.getHostGroups());
            }
        };

    }

    @Bean(name = "CLUSTER_MANAGER_START_SERVER_AGENT_STATE")
    public Action<?, ?> clusterManagerStartServerAndAgentAction() {
        return new AbstractClusterUpscaleAction<>(AmbariStopServerAndAgentResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, AmbariStopServerAndAgentResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.startClusterManagementServer(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new AmbariStartServerAndAgentRequest(context.getStackId(), context.getHostGroups());
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_STATE")
    public Action<?, ?> clusterManagerRegenerateKerberosKeytabsAction() {
        return new AbstractClusterUpscaleAction<>(StartServerAndAgentResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, StartServerAndAgentResult payload, Map<Object, Object> variables) {
                RegenerateKerberosKeytabsRequest request =
                        new RegenerateKerberosKeytabsRequest(context.getStackId(), context.getHostGroups(), context.getPrimaryGatewayHostName());
                if (isKerberosSecured(variables)) {
                    clusterUpscaleFlowService.regenerateKeytabs(context.getStackId());
                    sendEvent(context, request.selector(), request);
                } else {
                    RegenerateKerberosKeytabsResult result = new RegenerateKerberosKeytabsResult(request);
                    sendEvent(context, result.selector(), result);
                }
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_ENSURE_COMPONENTS_ARE_STOPPED_STATE")
    public Action<?, ?> ensureClusterComponentsAreStoppedAction() {
        return new AbstractClusterUpscaleAction<>(RegenerateKerberosKeytabsResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, RegenerateKerberosKeytabsResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterStopComponents(context.getStackId());
                Map<String, String> components = getInstalledComponents(variables);
                EnsureClusterComponentsAreStoppedRequest request =
                        new EnsureClusterComponentsAreStoppedRequest(context.getStackId(), context.getHostGroups(), context.getPrimaryGatewayHostName(),
                                components);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_INIT_COMPONENTS_STATE")
    public Action<?, ?> clusterManagerInitComponentsAction() {
        return new AbstractClusterUpscaleAction<>(ClusterManagerEnsureComponentsAreStoppedResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterManagerEnsureComponentsAreStoppedResult payload, Map<Object, Object> variables) {
                Map<String, String> components = getInstalledComponents(variables);
                ClusterManagerInitComponentsRequest request =
                        new ClusterManagerInitComponentsRequest(context.getStackId(), context.getHostGroups(),
                                context.getPrimaryGatewayHostName(), components);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_INSTALL_COMPONENTS_STATE")
    public Action<?, ?> clusterManagerInstallComponentsAction() {
        return new AbstractClusterUpscaleAction<>(ClusterManagerInitComponentsResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterManagerInitComponentsResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.reinstallClusterComponents(context.getStackId());
                Map<String, String> components = getInstalledComponents(variables);
                ClusterManagerInstallComponentsRequest request =
                        new ClusterManagerInstallComponentsRequest(context.getStackId(), context.getHostGroups(),
                                context.getPrimaryGatewayHostName(), components);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_START_COMPONENTS_STATE")
    public Action<?, ?> clusterManagerStartComponentsAction() {
        return new AbstractClusterUpscaleAction<>(ClusterManagerInstallComponentsResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterManagerInstallComponentsResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.startComponentsOnNewHosts(context.getStackId());
                Map<String, String> components = getInstalledComponents(variables);
                ClusterManagerStartComponentsRequest request = new ClusterManagerStartComponentsRequest(
                        context.getStackId(),
                        context.getHostGroups(),
                        context.getPrimaryGatewayHostName(),
                        components
                );
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_RESTART_ALL_STATE")
    public Action<?, ?> clusterManagerRestartAllAction() {
        return new AbstractClusterUpscaleAction<>(ClusterManagerStartComponentsResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterManagerStartComponentsResult payload, Map<Object, Object> variables) {
                AmbariRestartAllRequest request = new AmbariRestartAllRequest(context.getStackId(), context.getHostGroups(), isRollingRestartEnabled(variables));
                if (isKerberosSecured(variables) && !isSingleNodeCluster(variables)) {
                    clusterUpscaleFlowService.restartAllClusterComponents(context.getStackId());
                    sendEvent(context, request.selector(), request);
                } else {
                    ClusterManagerRestartAllResult result = new ClusterManagerRestartAllResult(request);
                    sendEvent(context, result.selector(), result);
                }
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_REPAIR_SINGLE_MASTER_FINISHED_STATE")
    public Action<?, ?> clusterManagerRepairSingleMasterFinishedAction() {
        return new AbstractClusterUpscaleAction<>(ClusterManagerRestartAllResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterManagerRestartAllResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterManagerRepairSingleMasterFinished(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                UpscaleClusterRequest request = new UpscaleClusterRequest(context.getStackId(), context.getHostGroups(),
                        context.isRepair(), context.isRestartServices(), context.getHostGroupWithAdjustment(), context.isSinglePrimaryGateway());
                return new UpscaleClusterResult(request);
            }
        };
    }

    @Bean(name = "EXECUTING_POSTRECIPES_STATE")
    public Action<?, ?> executePostRecipesAction() {
        return new AbstractClusterUpscaleAction<>(UpscaleClusterResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscaleClusterResult payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpscalePostRecipesRequest(context.getStackId(), context.getHostGroups(), context.getHostGroupWithAdjustment());
            }
        };
    }

    @Bean(name = "FINALIZE_UPSCALE_STATE")
    public Action<?, ?> upscaleFinishedAction() {
        return new AbstractClusterUpscaleAction<>(UpscalePostRecipesResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscalePostRecipesResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterUpscaleFinished(context.getStack(), context.getHostGroups(), context.isRepair());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL, context.getStack());
                meteringService.sendMeteringStatusChangeEventForStack(context.getStackId(), SCALE_UP);
                sendEvent(context, FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "CLUSTER_UPSCALE_FAILED_STATE")
    public Action<?, ?> clusterUpscaleFailedAction() {
        return new AbstractStackFailureAction<ClusterUpscaleState, ClusterUpscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterUpscaleFailed(context.getStackId(), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED, context.getStack(), payload.getException());
                ClusterUpscaleFailedConclusionRequest request = new ClusterUpscaleFailedConclusionRequest(context.getStackId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    private abstract static class AbstractClusterUpscaleAction<P extends Payload>
            extends AbstractStackAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, P> {

        // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
        @Deprecated
        static final String HOSTGROUPNAME = "HOSTGROUPNAME";

        // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
        @Deprecated
        static final String ADJUSTMENT = "ADJUSTMENT";

        static final String HOST_GROUP_WITH_ADJUSTMENT = "HOST_GROUP_WITH_ADJUSTMENT";

        static final String SINGLE_PRIMARY_GATEWAY = "SINGLE_PRIMARY_GATEWAY";

        static final String KERBEROS_SECURED = "KERBEROS_SECURED";

        static final String HOST_NAME = "HOST_NAME";

        static final String INSTALLED_COMPONENTS = "INSTALLED_COMPONENTS";

        static final String SINGLE_NODE_CLUSTER = "SINGLE_NODE_CLUSTER";

        static final String CLUSTER_MANAGER_TYPE = "CLUSTER_MANAGER_TYPE";

        static final String RESTART_SERVICES = "RESTART_SERVICES";

        static final String HOST_NAMES_BY_HOST_GROUP = "HOST_NAMES_BY_HOST_GROUP";

        static final String REPAIR = "REPAIR";

        static final String ROLLING_RESTART_ENABLED = "ROLLING_RESTART_ENABLED";

        @Inject
        private StackDtoService stackDtoService;

        AbstractClusterUpscaleAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<ClusterUpscaleContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }

        @Override
        protected ClusterUpscaleContext createFlowContext(FlowParameters flowParameters, StateContext<ClusterUpscaleState,
                ClusterUpscaleEvent> stateContext, P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
            ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
            MDCBuilder.buildMdcContext(cluster);
            return new ClusterUpscaleContext(flowParameters, stack, cluster, getHostGroupWithAdjustment(variables), isSinglePrimaryGateway(variables),
                    getPrimaryGatewayHostName(variables), getClusterManagerType(variables), isRepair(variables), isRestartServices(variables));
        }

        private Map<String, Integer> getHostGroupWithAdjustment(Map<Object, Object> variables) {
            if (!variables.containsKey(HOST_GROUP_WITH_ADJUSTMENT) && variables.containsKey(HOSTGROUPNAME) && variables.containsKey(ADJUSTMENT)) {
                variables.put(HOST_GROUP_WITH_ADJUSTMENT, Map.of(variables.get(HOSTGROUPNAME), variables.get(ADJUSTMENT)));
            }
            return (Map<String, Integer>) variables.get(HOST_GROUP_WITH_ADJUSTMENT);
        }

        private Boolean isSinglePrimaryGateway(Map<Object, Object> variables) {
            return (Boolean) variables.get(SINGLE_PRIMARY_GATEWAY);
        }

        private String getPrimaryGatewayHostName(Map<Object, Object> variables) {
            return (String) variables.get(HOST_NAME);
        }

        Map<String, Collection<String>> getHostNamesByHostGroup(Map<Object, Object> variables) {
            return (Map<String, Collection<String>>) variables.getOrDefault(HOST_NAMES_BY_HOST_GROUP, new HashMap<>());
        }

        Map<String, String> getInstalledComponents(Map<Object, Object> variables) {
            return (Map<String, String>) variables.get(INSTALLED_COMPONENTS);
        }

        Boolean isKerberosSecured(Map<Object, Object> variables) {
            return (Boolean) variables.get(KERBEROS_SECURED);
        }

        private boolean isRepair(Map<Object, Object> variables) {
            return variables.get(REPAIR) != null && (Boolean) variables.get(REPAIR);
        }

        Boolean isRestartServices(Map<Object, Object> variables) {
            return (Boolean) variables.get(RESTART_SERVICES);
        }

        Boolean isSingleNodeCluster(Map<Object, Object> variables) {
            return (Boolean) variables.get(SINGLE_NODE_CLUSTER);
        }

        ClusterManagerType getClusterManagerType(Map<Object, Object> variables) {
            return (ClusterManagerType) variables.get(CLUSTER_MANAGER_TYPE);
        }

        boolean isRollingRestartEnabled(Map<Object, Object> variables) {
            return (Boolean) variables.getOrDefault(ROLLING_RESTART_ENABLED, false);
        }
    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerEnsureComponentsAreStoppedResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRepairSingleMasterStartResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRestartAllRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerRestartAllResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStartServerAndAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStopComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.EnsureClusterComponentsAreStoppedRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopClusterComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class ClusterUpscaleActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleActions.class);

    @Inject
    private ClusterUpscaleFlowService clusterUpscaleFlowService;

    @Bean(name = "UPLOAD_UPSCALE_RECIPES_STATE")
    public Action<?, ?> uploadUpscaleRecipesAction() {
        return new AbstractClusterUpscaleAction<>(ClusterScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTGROUPNAME, payload.getHostGroupName());
                variables.put(ADJUSTMENT, payload.getAdjustment());
                variables.put(SINGLE_PRIMARY_GATEWAY, payload.isSinglePrimaryGateway());
                variables.put(KERBEROS_SECURED, payload.isKerberosSecured());
                variables.put(SINGLE_NODE_CLUSTER, payload.isSingleNodeCluster());
                variables.put(CLUSTER_MANAGER_TYPE, payload.getClusterManagerType());
                if (payload.isSinglePrimaryGateway()) {
                    variables.put(HOST_NAME, getMasterHostname(payload));
                }
            }

            private String getMasterHostname(ClusterScaleTriggerEvent payload) {
                return payload.getHostNames().iterator().hasNext()
                        ? payload.getHostNames().iterator().next()
                        : "";
            }

            @Override
            protected void doExecute(ClusterUpscaleContext context, ClusterScaleTriggerEvent payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.upscalingClusterManager(context.getStackId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UploadUpscaleRecipesRequest(context.getStackId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "RECONFIGURE_KEYTABS_STATE")
    public Action<?, ?> configureKeytabsAction() {
        return new AbstractClusterUpscaleAction<>(UploadUpscaleRecipesResult.class) {
            @Override
            protected void doExecute(ClusterUpscaleContext context, UploadUpscaleRecipesResult payload, Map<Object, Object> variables) {
                if (context.isSinglePrimaryGateway() && ClusterManagerType.CLOUDERA_MANAGER.equals(context.getClusterManagerType())) {
                    KeytabConfigurationRequest keytabConfigurationRequest = new KeytabConfigurationRequest(context.getStackId());
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
                return new UpscaleCheckHostMetadataRequest(context.getStackId(), context.getHostGroupName(),
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
                return new UpscaleClusterManagerRequest(context.getStackId(), context.getHostGroupName(), context.getAdjustment(),
                        context.isSinglePrimaryGateway());
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
                            new AmbariRepairSingleMasterStartResult(context.getStackId(), context.getHostGroupName());
                    sendEvent(context, result.selector(), result);
                } else {
                    UpscaleClusterRequest request = new UpscaleClusterRequest(context.getStackId(), context.getHostGroupName());
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
                return new AmbariGatherInstalledComponentsRequest(context.getStackId(), context.getHostGroupName(), context.getPrimaryGatewayHostName());
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
                        new ClusterManagerStopComponentsRequest(context.getStackId(), context.getHostGroupName(),
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
                return new AmbariStopServerAndAgentRequest(context.getStackId(), context.getHostGroupName());
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
                return new AmbariStartServerAndAgentRequest(context.getStackId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_STATE")
    public Action<?, ?> clusterManagerRegenerateKerberosKeytabsAction() {
        return new AbstractClusterUpscaleAction<>(StartServerAndAgentResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, StartServerAndAgentResult payload, Map<Object, Object> variables) {
                RegenerateKerberosKeytabsRequest request =
                        new RegenerateKerberosKeytabsRequest(context.getStackId(), context.getHostGroupName(), context.getPrimaryGatewayHostName());
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
                        new EnsureClusterComponentsAreStoppedRequest(context.getStackId(), context.getHostGroupName(), context.getPrimaryGatewayHostName(),
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
                        new ClusterManagerInitComponentsRequest(context.getStackId(), context.getHostGroupName(),
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
                        new ClusterManagerInstallComponentsRequest(context.getStackId(), context.getHostGroupName(),
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
                        context.getHostGroupName(),
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
                AmbariRestartAllRequest request = new AmbariRestartAllRequest(context.getStackId(), context.getHostGroupName());
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
                UpscaleClusterRequest request = new UpscaleClusterRequest(context.getStackId(), context.getHostGroupName());
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
                return new UpscalePostRecipesRequest(context.getStackId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "FINALIZE_UPSCALE_STATE")
    public Action<?, ?> upscaleFinishedAction() {
        return new AbstractClusterUpscaleAction<>(UpscalePostRecipesResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscalePostRecipesResult payload, Map<Object, Object> variables) {
                clusterUpscaleFlowService.clusterUpscaleFinished(context.getStack(), payload.getHostGroupName());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL, context.getStack());
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
                clusterUpscaleFlowService.clusterUpscaleFailed(context.getStackView().getId(), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED, context.getStackView(), payload.getException());
                sendEvent(context, FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractClusterUpscaleAction<P extends Payload>
            extends AbstractStackAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, P> {
        static final String HOSTGROUPNAME = "HOSTGROUPNAME";

        static final String ADJUSTMENT = "ADJUSTMENT";

        static final String SINGLE_PRIMARY_GATEWAY = "SINGLE_PRIMARY_GATEWAY";

        static final String KERBEROS_SECURED = "KERBEROS_SECURED";

        static final String HOST_NAME = "HOST_NAME";

        static final String INSTALLED_COMPONENTS = "INSTALLED_COMPONENTS";

        static final String SINGLE_NODE_CLUSTER = "SINGLE_NODE_CLUSTER";

        static final String CLUSTER_MANAGER_TYPE = "CLUSTER_MANAGER_TYPE";

        @Inject
        private StackService stackService;

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
            StackView stack = stackService.getViewByIdWithoutAuth(payload.getResourceId());
            MDCBuilder.buildMdcContext(stack.getClusterView());
            return new ClusterUpscaleContext(flowParameters, stack, getHostgroupName(variables), getAdjustment(variables),
                    isSinglePrimaryGateway(variables), getPrimaryGatewayHostName(variables), getClusterManagerType(variables));
        }

        private String getHostgroupName(Map<Object, Object> variables) {
            return (String) variables.get(HOSTGROUPNAME);
        }

        private Integer getAdjustment(Map<Object, Object> variables) {
            return (Integer) variables.get(ADJUSTMENT);
        }

        private Boolean isSinglePrimaryGateway(Map<Object, Object> variables) {
            return (Boolean) variables.get(SINGLE_PRIMARY_GATEWAY);
        }

        private String getPrimaryGatewayHostName(Map<Object, Object> variables) {
            return (String) variables.get(HOST_NAME);
        }

        Map<String, String> getInstalledComponents(Map<Object, Object> variables) {
            return (Map<String, String>) variables.get(INSTALLED_COMPONENTS);
        }

        Boolean isKerberosSecured(Map<Object, Object> variables) {
            return (Boolean) variables.get(KERBEROS_SECURED);
        }

        Boolean isSingleNodeCluster(Map<Object, Object> variables) {
            return (Boolean) variables.get(SINGLE_NODE_CLUSTER);
        }

        ClusterManagerType getClusterManagerType(Map<Object, Object> variables) {
            return (ClusterManagerType) variables.get(CLUSTER_MANAGER_TYPE);
        }

    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent.FINALIZED_EVENT;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.ClusterUpscalePayload;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterUpscaleResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AddClusterContainersResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ConfigureSssdRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ConfigureSssdResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePreRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePreRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallFsRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallFsRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.WaitForAmbariHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.WaitForAmbariHostsResult;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterUpscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleActions.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "INSTALL_FS_RECIPES_STATE")
    public Action installFsRecipesAction() {
        return new AbstractClusterUpscaleAction<AddClusterContainersResult>(AddClusterContainersResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final AddClusterContainersResult payload, Map<Object, Object> variables)
                    throws Exception {
                flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.AMBARI_CLUSTER_SCALING_UP, UPDATE_IN_PROGRESS.name());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new InstallFsRecipesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "WAIT_FOR_AMBARI_HOSTS_STATE")
    public Action waitForAmbariHostsAction() {
        return new AbstractClusterUpscaleAction<InstallFsRecipesResult>(InstallFsRecipesResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final InstallFsRecipesResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new WaitForAmbariHostsRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "CONFIGURE_SSSD_STATE")
    public Action configureSssdAction() {
        return new AbstractClusterUpscaleAction<WaitForAmbariHostsResult>(WaitForAmbariHostsResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final WaitForAmbariHostsResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new ConfigureSssdRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "INSTALL_RECIPES_STATE")
    public Action installRecipesAction() {
        return new AbstractClusterUpscaleAction<ConfigureSssdResult>(ConfigureSssdResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final ConfigureSssdResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new InstallRecipesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "EXECUTE_PRE_RECIPES_STATE")
    public Action executePreRecipesAction() {
        return new AbstractClusterUpscaleAction<InstallRecipesResult>(InstallRecipesResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final InstallRecipesResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new ExecutePreRecipesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "INSTALL_SERVICES_STATE")
    public Action installServicesAction() {
        return new AbstractClusterUpscaleAction<ExecutePreRecipesResult>(ExecutePreRecipesResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final ExecutePreRecipesResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new InstallServicesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "EXECUTE_POST_RECIPES_STATE")
    public Action executePostRecipesAction() {
        return new AbstractClusterUpscaleAction<InstallServicesResult>(InstallServicesResult.class) {

            @Override
            protected void doExecute(final ClusterUpscaleContext context, final InstallServicesResult payload, Map<Object, Object> variables)
                    throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new ExecutePostRecipesRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "UPDATE_METADATA_STATE")
    public Action updateMetadataAction() {
        return new AbstractClusterUpscaleAction<ExecutePostRecipesResult>(ExecutePostRecipesResult.class) {

            @Override
            protected void doExecute(ClusterUpscaleContext context, ExecutePostRecipesResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return new UpdateMetadataRequest(context.getStack().getId(), context.getHostGroupName());
            }
        };
    }

    @Bean(name = "FINALIZE_STATE")
    public Action upscaleFinishedAction() {
        return new AbstractClusterUpscaleAction<UpdateMetadataResult>(UpdateMetadataResult.class) {

            @Inject
            private EmailSenderService emailSenderService;

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpdateMetadataResult payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                int failedHosts = payload.getFailedHosts();
                boolean success = failedHosts == 0;
                if (success) {
                    LOGGER.info("Cluster upscaled successfully");
                    clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
                    flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALED_UP, AVAILABLE.name());
                    if (stack.getCluster().getEmailNeeded()) {
                        emailSenderService.sendUpscaleSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                        flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.AMBARI_CLUSTER_NOTIFICATION_EMAIL, AVAILABLE.name());
                    }
                } else {
                    LOGGER.info("Cluster upscale failed. {} hosts failed to upscale", failedHosts);
                    clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED);
                    flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added",
                            String.format("Ambari upscale operation failed on %d node(s).", failedHosts));
                }
                sendEvent(context.getFlowId(), FINALIZED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "FAILED_STATE")
    public Action clusterupscaleFailedAction() {
        return new AbstractClusterUpscaleAction<UpscaleClusterFailedPayload>(UpscaleClusterFailedPayload.class) {

            @Inject
            private StackUpdater stackUpdater;

            @Override
            protected void doExecute(ClusterUpscaleContext context, UpscaleClusterFailedPayload payload, Map<Object, Object> variables) throws Exception {
                Exception errorDetails = payload.getErrorDetails();
                LOGGER.error("Error during Cluster upscale flow: " + errorDetails.getMessage(), errorDetails);
                Stack stack = context.getStack();
                clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, errorDetails.getMessage());
                stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("New node(s) could not be added to the cluster: %s",
                        errorDetails));
                flowMessageService.fireEventAndLog(stack.getId(), Msg.AMBARI_CLUSTER_SCALING_FAILED, UPDATE_FAILED.name(), "added", errorDetails);
                sendEvent(context.getFlowId(), FAIL_HANDLED_EVENT.stringRepresentation(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<UpscaleClusterFailedPayload>> payloadConverters) {
                payloadConverters.add(new PayloadConverter<UpscaleClusterFailedPayload>() {
                    @Override
                    public boolean canConvert(Class sourceClass) {
                        return AbstractClusterUpscaleResult.class.isAssignableFrom(sourceClass);
                    }

                    @Override
                    public UpscaleClusterFailedPayload convert(Object payload) {
                        AbstractClusterUpscaleResult result = (AbstractClusterUpscaleResult) payload;
                        return new UpscaleClusterFailedPayload(result.getStackId(), result.getHostGroupName(), result.getErrorDetails());
                    }
                });
            }

            @Override
            protected Selectable createRequest(ClusterUpscaleContext context) {
                return null;
            }
        };
    }

    private abstract class AbstractClusterUpscaleAction<P extends ClusterUpscalePayload>
            extends AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, P> {

        @Inject
        private StackService stackService;

        protected AbstractClusterUpscaleAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected Object getFailurePayload(ClusterUpscaleContext flowContext, Exception ex) {
            return new UpscaleClusterFailedPayload(flowContext.getStack().getId(), flowContext.getHostGroupName(), ex);
        }

        @Override
        protected ClusterUpscaleContext createFlowContext(StateContext<ClusterUpscaleState, ClusterUpscaleEvent> stateContext, P payload) {
            String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
            Stack stack = stackService.getById(payload.getStackId());
            MDCBuilder.buildMdcContext(stack);
            return new ClusterUpscaleContext(flowId, stack, payload.getHostGroupName());
        }
    }
}
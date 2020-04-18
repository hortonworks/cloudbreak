package com.sequenceiq.periscope.monitor.evaluator.cm;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.MonitoredStack;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.SecurityConfigService;
import com.sequenceiq.periscope.service.security.TlsConfigurationException;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component("ClouderaManagerClusterCreationEvaluator")
@Scope("prototype")
public class ClouderaManagerClusterCreationEvaluator extends ClusterCreationEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterCreationEvaluator.class);

    private static final String EVALUATOR_NAME = ClouderaManagerClusterCreationEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private RequestLogging requestLogging;

    @Inject
    private SecretService secretService;

    private EvaluatorContext context;

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        AutoscaleStackV4Response stack = (AutoscaleStackV4Response) context.getData();
        try {
            Cluster cluster = clusterService.findOneByStackId(stack.getStackId());
            Long clusterId = null;
            if (cluster != null) {
                clusterId = cluster.getId();
            }
            MonitoredStack resolvedStack = createMonitoredStack(stack, clusterId);
            if (cluster != null) {
                cmHealthCheck(resolvedStack);
                updateCluster(stack, cluster, resolvedStack);
            } else {
                clusterService.validateClusterUniqueness(resolvedStack);
                createCluster(stack, resolvedStack);
            }
        } catch (ClusterManagerHealthCheckException ahf) {
            LOGGER.info("Cloudera Manager health check failed for Cloudbreak stack: {} (CRN:{}). Original message: {}", stack.getStackCrn(), stack.getName(),
                    ahf.getMessage());
        } catch (TlsConfigurationException ex) {
            LOGGER.error("Could not prepare TLS configuration for Cloudbreak stack: {} (CRN:{}). Original message: {}", stack.getStackCrn(), stack.getName(),
                    ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn(String.format("Could not create cluster for Cloudbreak stack: %s (CRN:%s)", stack.getStackCrn(), stack.getName()), ex);
        } finally {
            LOGGER.debug("Finished clusterCreationEvaluator in {} ms", System.currentTimeMillis() - start);
        }
    }

    @Override
    public void setContext(EvaluatorContext context) {
        this.context = context;
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return context;
    }

    private void createCluster(AutoscaleStackV4Response stack, MonitoredStack monitoredStack) {
        LOGGER.debug("Creating cluster for Cloudera Manager host: {}", monitoredStack.getClusterManager().getHost());
        Cluster cluster = clusterService.create(monitoredStack, RUNNING,
                new ClusterPertain(stack.getTenant(), stack.getWorkspaceId(), stack.getUserId(), stack.getUserCrn()));
        MDCBuilder.buildMdcContext(cluster);
        History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);

        notificationSender.send(cluster, history);
    }

    private void updateCluster(AutoscaleStackV4Response stack, Cluster cluster, MonitoredStack monitoredStack) {
        if (PENDING.equals(cluster.getState()) || SUSPENDED.equals(cluster.getState())) {
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Update cluster and set it's state to 'RUNNING' for Cloudera Manager host: {}", monitoredStack.getClusterManager().getHost());
            cluster = clusterService.update(cluster.getId(), monitoredStack, RUNNING, cluster.isAutoscalingEnabled());
            History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
            notificationSender.send(cluster, history);
        }
    }

    private MonitoredStack createMonitoredStack(AutoscaleStackV4Response stack, Long clusterId) {
        String host = stack.getAmbariServerIp();
        String gatewayPort = String.valueOf(stack.getGatewayPort());
        SecurityConfig securityConfig = null;
        if (clusterId != null) {
            securityConfig = securityConfigService.getSecurityConfig(clusterId);
        }
        ClusterManager clusterManager =
                new ClusterManager(host, gatewayPort, stack.getUserNamePath(), stack.getPasswordPath(), ClusterManagerVariant.CLOUDERA_MANAGER);
        return new MonitoredStack(clusterManager, stack.getName(), stack.getStackCrn(), stack.getCloudPlatform(), stack.getStackType(),
                stack.getStackId(), securityConfig, stack.getTunnel());
    }

    private void cmHealthCheck(MonitoredStack monitoredStack) {
        ClusterManager cm = monitoredStack.getClusterManager();
        String host = cm.getHost();
        try {
            HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(monitoredStack.getStackCrn(), cm.getHost(),
                    monitoredStack.getTunnel());
            String user = secretService.get(cm.getUser());
            String pass = secretService.get(cm.getPass());
            ApiClient client = clouderaManagerApiClientProvider.getClient(Integer.valueOf(cm.getPort()), user, pass, httpClientConfig);
            ClouderaManagerResourceApi resourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            Boolean healthCheckResult = requestLogging.logResponseTime(() -> {
                try {
                    ApiVersionInfo version = resourceApi.getVersion();
                    return StringUtils.isNotEmpty(version.getVersion());
                } catch (ApiException e) {
                    throw new ClusterManagerHealthCheckException("Failed to connect to Cloudera Manager host", e);
                }
            }, "healthCheck");
            if (!healthCheckResult) {
                throw new ClusterManagerHealthCheckException(String.format("Cloudera Manager on host '%s' is not running.", host));
            }
        } catch (Exception ex) {
            throw new ClusterManagerHealthCheckException(String.format("Health check failed on host '%s':", host), ex);
        }
    }

    @Override
    public ClusterManagerVariant getSupportedClusterManagerVariant() {
        return ClusterManagerVariant.CLOUDERA_MANAGER;
    }

    private static class ClusterManagerHealthCheckException extends RuntimeException {
        ClusterManagerHealthCheckException(String message) {
            super(message);
        }

        ClusterManagerHealthCheckException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

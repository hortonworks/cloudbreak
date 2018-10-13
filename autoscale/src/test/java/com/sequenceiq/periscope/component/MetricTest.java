package com.sequenceiq.periscope.component;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.periscope.component.MetricTest.PERISCOPE_NODE_ID;
import static com.sequenceiq.periscope.component.MetricTest.THREADPOOL_MAX_SIZE;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JndiConnectionFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleEndpoint;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.service.CrudRepositoryLookupService;
import com.sequenceiq.cloudbreak.service.TransactionExecutorService;
import com.sequenceiq.periscope.PeriscopeApplication;
import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.config.DatabaseConfig;
import com.sequenceiq.periscope.config.DatabaseMigrationConfig;
import com.sequenceiq.periscope.controller.AutoScaleClusterV1Controller;
import com.sequenceiq.periscope.controller.AutoScaleClusterV2Controller;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.LoggedExecutorService;
import com.sequenceiq.periscope.monitor.handler.UpdateFailedHandler;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.HistoryRepository;
import com.sequenceiq.periscope.repository.MetricAlertRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.repository.PrometheusAlertRepository;
import com.sequenceiq.periscope.repository.ScalingPolicyRepository;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;
import com.sequenceiq.periscope.repository.SubscriptionRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.repository.UserRepository;
import com.sequenceiq.periscope.service.AuthenticatedUserService;
import com.sequenceiq.periscope.service.MetricService;
import com.sequenceiq.periscope.service.ha.LeaderElectionService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeConfig;
import com.sequenceiq.periscope.service.security.CachedUserDetailsService;
import com.zaxxer.hikari.HikariDataSource;

import springfox.documentation.swagger.web.SwaggerResourcesProvider;

@SpringBootTest(classes = MetricTest.TestConfig.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "periscope.client.id=periscope",
        "periscope.client.secret=CLientSecret",
        "periscope.identity.server.url=1.2.3.4",
        "periscope.db.port.5432.tcp.addr=1.2.3.4",
        "periscope.db.port.5432.tcp.port=5432",
        "periscope.cloudbreak.url=http://1.2.3.4:8080",
        "periscope.instance.node.id=" + PERISCOPE_NODE_ID,
        "server.port=8079",
        "cb.jwt.signKey=signKey",
        "cb.etc.config.dir=/etcConfigDir",
        "periscope.threadpool.core.size=" + THREADPOOL_MAX_SIZE,
        "periscope.threadpool.max.size=" + THREADPOOL_MAX_SIZE,
        "periscope.threadpool.queue.size=10"
})
public class MetricTest {

    static final String PERISCOPE_NODE_ID = "1";

    static final int THREADPOOL_MAX_SIZE = 5;

    private static final String PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE = "periscope.cluster.state.active";

    private static final String PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED = "periscope.cluster.state.suspended";

    private static final String THREADPOOL_QUEUE_SIZE = "periscope.threadpool.queue.size";

    private static final String THREADPOOL_ACTIVE_THREADS = "periscope.threadpool.threads.active";

    private static final String THREADPOOL_THREADS_TOTAL = "periscope.threadpool.threads.coresize";

    private static final String THREADPOOL_TASKS_COMPLETED = "periscope.threadpool.tasks.completed";

    private static final double DELTA = 0.0001;

    private static final String USER_A_ID = "userAId";

    private static final String ACCOUNT_A = "ACCOUNT_A";

    private static final String USER_A_EMAIL = "userA@any.com";

    private static final long STACK_ID = 2L;

    private static final long CLUSTER_ID = 1L;

    private static final String PERISCOPE_METRICS_LEADER = "periscope.node.leader";

    private final List<String> counterMetrics = Arrays.asList(
            "periscope.cluster.upscale.triggered",
            "periscope.cluster.downscale.triggered",
            "periscope.cluster.upscale.successful",
            "periscope.cluster.upscale.failed",
            "periscope.cluster.downscale.successful",
            "periscope.cluster.downscale.failed"
    );

    private final List<String> expectedGaugeMetrics = Arrays.asList(
            PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE,
            PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED,
            PERISCOPE_METRICS_LEADER,
            THREADPOOL_ACTIVE_THREADS,
            THREADPOOL_QUEUE_SIZE,
            THREADPOOL_TASKS_COMPLETED,
            THREADPOOL_THREADS_TOTAL
    );

    @Inject
    private AutoScaleClusterV1Controller autoScaleClusterV1Controller;

    @Inject
    private AutoScaleClusterV2Controller autoScaleClusterV2Controller;

    @LocalServerPort
    private int port;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private TestRestTemplate restTemplate;

    @MockBean
    private AuthenticatedUserService authenticatedUserService;

    @MockBean
    private CloudbreakClient cloudbreakClient;

    @MockBean
    private AutoscaleEndpoint autoscaleEndpoint;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private HistoryRepository historyRepository;

    @Inject
    private MetricService metricService;

    @Inject
    private LeaderElectionService leaderElectionService;

    @Inject
    private PeriscopeNodeRepository periscopeNodeRepository;

    @Inject
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Inject
    private UpdateFailedHandler updateFailedHandler;

    @Inject
    private LoggedExecutorService loggedExecutorService;

    @Test
    public void testCounterMetricsPresent() {
        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());

        List<String> missingMetrics = counterMetrics.stream().filter(x -> !metrics.containsKey(toReportedMetricName(x, true))).collect(Collectors.toList());
        assertThat(missingMetrics, empty());
    }

    @Test
    public void testGaugeMetricsPresent() {
        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());

        List<String> missingMetrics = expectedGaugeMetrics.stream()
                .filter(x -> !metrics.containsKey(toReportedMetricName(x, false)))
                .collect(Collectors.toList());
        assertThat("Some gauge metrics are not present", missingMetrics, empty());
    }

    @Test
    public void testMetricsWhenAddActiveCluster() {
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(autoscaleEndpoint.get(eq(STACK_ID))).thenReturn(getStackResponse(AVAILABLE, AVAILABLE));
        when(authenticatedUserService.getPeriscopeUser()).thenReturn(getOwnerUser());
        when(clusterRepository.save(any())).thenAnswer(this::saveCluster);
        when(historyRepository.save(any())).then(returnsFirstArg());
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString()))
                .thenReturn(1);
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString()))
                .thenReturn(0);

        autoScaleClusterV1Controller.addCluster(getCreateAutoscaleClusterRequest());

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());
        assertEquals(1.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE, false)).get(0)), DELTA);
        assertEquals(0.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED, false)).get(0)), DELTA);
        InOrder inOrder = Mockito.inOrder(clusterRepository);
        inOrder.verify(clusterRepository).save(argThat(Cluster::isRunning));
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString());
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString());
    }

    @Test
    public void testMetricsWhenSuspendActiveCluster() {
        metricService.submitGauge(MetricType.CLUSTER_STATE_ACTIVE, 1);
        metricService.submitGauge(MetricType.CLUSTER_STATE_SUSPENDED, 0);
        when(clusterRepository.findByStackId(STACK_ID)).thenReturn(getACluster(ClusterState.RUNNING));
        when(clusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(getACluster(ClusterState.RUNNING)));
        when(authenticatedUserService.getPeriscopeUser()).thenReturn(getOwnerUser());
        when(clusterRepository.save(any())).thenAnswer(this::saveCluster);
        when(historyRepository.save(any())).then(returnsFirstArg());
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString()))
                .thenReturn(0);
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString()))
                .thenReturn(1);

        autoScaleClusterV2Controller.suspendByCloudbreakCluster(STACK_ID);

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());
        assertEquals(0.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE, false)).get(0)), DELTA);
        assertEquals(1.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED, false)).get(0)), DELTA);
        InOrder inOrder = Mockito.inOrder(clusterRepository);
        inOrder.verify(clusterRepository).save(argThat(x -> x.getId() == 1L && !x.isRunning()));
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString());
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString());
    }

    @Test
    public void testMetricsWhenRunSuspendedCluster() {
        metricService.submitGauge(MetricType.CLUSTER_STATE_SUSPENDED, 1);
        when(clusterRepository.findByStackId(STACK_ID)).thenReturn(getACluster(ClusterState.SUSPENDED));
        when(clusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(getACluster(ClusterState.SUSPENDED)));
        when(authenticatedUserService.getPeriscopeUser()).thenReturn(getOwnerUser());
        when(clusterRepository.save(any())).thenAnswer(this::saveCluster);
        when(historyRepository.save(any())).then(returnsFirstArg());
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString()))
                .thenReturn(1);
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString()))
                .thenReturn(0);

        autoScaleClusterV2Controller.runByCloudbreakCluster(STACK_ID);

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());
        assertEquals(1.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE, false)).get(0)), DELTA);
        assertEquals(0.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED, false)).get(0)), DELTA);
        InOrder inOrder = Mockito.inOrder(clusterRepository);
        inOrder.verify(clusterRepository).save(argThat(x -> x.getId() == CLUSTER_ID && x.isRunning()));
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString());
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString());
    }

    @Test
    public void testMetricsWhenAutofailSuspendsCluster() throws InterruptedException {
        metricService.submitGauge(MetricType.CLUSTER_STATE_ACTIVE, 2);
        when(clusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(getACluster(ClusterState.RUNNING)));
        when(clusterRepository.save(any())).thenAnswer(this::saveCluster);
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString()))
                .thenReturn(0);
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString()))
                .thenReturn(1);
        when(cloudbreakClient.autoscaleEndpoint()).thenReturn(autoscaleEndpoint);
        when(autoscaleEndpoint.get(eq(STACK_ID))).thenReturn(getStackResponse(AVAILABLE, AVAILABLE));
        ReflectionTestUtils.setField(updateFailedHandler, "updateFailures", getUpdateFailures(CLUSTER_ID));

        updateFailedHandler.onApplicationEvent(new UpdateFailedEvent(CLUSTER_ID));

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());
        assertEquals(0.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE, false)).get(0)), DELTA);
        assertEquals(1.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED, false)).get(0)), DELTA);
        InOrder inOrder = Mockito.inOrder(clusterRepository);
        inOrder.verify(clusterRepository).save(argThat(x -> x.getId() == CLUSTER_ID && !x.isRunning()));
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString());
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString());
    }

    private Map<Long, Integer> getUpdateFailures(Long clusterId) {
        Map<Long, Integer> updateFailures = new HashMap<>();
        updateFailures.put(clusterId, (Integer) ReflectionTestUtils.getField(updateFailedHandler, "RETRY_THRESHOLD") - 1);
        return updateFailures;
    }

    @Test
    public void testDeleteActiveCluster() {
        metricService.submitGauge(MetricType.CLUSTER_STATE_ACTIVE, 1);
        when(clusterRepository.findByStackId(STACK_ID)).thenReturn(getACluster(ClusterState.RUNNING));
        when(clusterRepository.findById(CLUSTER_ID)).thenReturn(Optional.of(getACluster(ClusterState.RUNNING)));
        when(authenticatedUserService.getPeriscopeUser()).thenReturn(getOwnerUser());
        when(clusterRepository.save(any())).thenAnswer(this::saveCluster);
        when(historyRepository.save(any())).then(returnsFirstArg());
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString()))
                .thenReturn(0);
        when(clusterRepository.countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString()))
                .thenReturn(0);

        autoScaleClusterV2Controller.deleteByCloudbreakCluster(STACK_ID);

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());
        assertEquals(0.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_ACTIVE, false)).get(0)), DELTA);
        assertEquals(0.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_CLUSTER_STATE_SUSPENDED, false)).get(0)), DELTA);
        InOrder inOrder = Mockito.inOrder(clusterRepository);
        inOrder.verify(clusterRepository).delete(argThat(x -> x.getId() == CLUSTER_ID && x.isRunning()));
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.RUNNING), eq(true), anyString());
        inOrder.verify(clusterRepository).countByStateAndAutoscalingEnabledAndPeriscopeNodeId(eq(ClusterState.SUSPENDED), eq(true), anyString());
    }

    @Test
    public void testLeaderElection() {
        metricService.submitGauge(MetricType.LEADER, 0);
        PeriscopeNode periscopeNode = new PeriscopeNode();
        when(periscopeNodeRepository.findById(periscopeNodeConfig.getId())).thenReturn(Optional.of(periscopeNode));

        leaderElectionService.leaderElection();

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());
        assertEquals(1.0, Double.parseDouble(metrics.get(toReportedMetricName(PERISCOPE_METRICS_LEADER, false)).get(0)), DELTA);
    }

    @Test
    public void testThreadpoolMetricsWhenTaskIsSubmitted() {
        BlockingTaskManager blockingTaskManager = new BlockingTaskManager();
        loggedExecutorService.submit("fakeTaskDone", () -> {
        });
        for (int t = 0; t < THREADPOOL_MAX_SIZE; t++) {
            loggedExecutorService.submit("fakeTaskActive" + t, blockingTaskManager.getNewTask());
        }
        loggedExecutorService.submit("fakeTaskInQueue", blockingTaskManager.getNewTask());

        MultiValueMap<String, String> metrics = responseToMap(readMetricsEndpoint());

        blockingTaskManager.releaseAll().waitAll();
        String threadpoolSize = String.format("%.1f", (float) THREADPOOL_MAX_SIZE);
        assertEquals(metrics.get(toReportedMetricName(THREADPOOL_ACTIVE_THREADS, false)).get(0), threadpoolSize);
        assertEquals(metrics.get(toReportedMetricName(THREADPOOL_QUEUE_SIZE, false)).get(0), "1.0");
        assertEquals(metrics.get(toReportedMetricName(THREADPOOL_TASKS_COMPLETED, false)).get(0), "1.0");
        assertEquals(metrics.get(toReportedMetricName(THREADPOOL_THREADS_TOTAL, false)).get(0), threadpoolSize);
    }

    private Cluster saveCluster(InvocationOnMock i) {
        Cluster cluster = i.getArgument(0);
        cluster.setUser(getOwnerUser());
        cluster.setId(CLUSTER_ID);
        return cluster;
    }

    private AutoscaleClusterRequest getCreateAutoscaleClusterRequest() {
        return new AutoscaleClusterRequest("host", "port", USER_A_ID, "pass", STACK_ID, true);
    }

    private PeriscopeUser getOwnerUser() {
        return new PeriscopeUser(USER_A_ID, USER_A_EMAIL, ACCOUNT_A);
    }

    private Cluster getACluster(ClusterState clusterState) {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setStackId(STACK_ID);
        cluster.setUser(getOwnerUser());
        cluster.setAmbari(new Ambari("host", "port", USER_A_ID, ""));
        cluster.setState(clusterState);
        return cluster;
    }

    private StackResponse getStackResponse(Status stackStatus, Status clusterStatus) {
        StackResponse stackResponse = new StackResponse();
        stackResponse.setStatus(stackStatus);
        stackResponse.setCluster(new ClusterResponse());
        stackResponse.getCluster().setStatus(clusterStatus);
        return stackResponse;
    }

    private String toReportedMetricName(String metricName, boolean counter) {
        String transformedMetricName = metricName.replaceAll("\\.", "_");
        return counter ? transformedMetricName + "_total" : transformedMetricName;
    }

    private String readMetricsEndpoint() {
        return restTemplate.getForObject("http://localhost:" + port + "/as/metrics", String.class);
    }

    private MultiValueMap<String, String> responseToMap(String response) {
        MultiValueMap<String, String> metrics = new LinkedMultiValueMap<>();
        for (String x : response.split("\n")) {
            String splitExpression = " ";
            if (x.contains("{")) {
                splitExpression = "\\{";
            }
            String[] keyValuePair = x.split(splitExpression);
            metrics.add(keyValuePair[0], keyValuePair[1]);
        }
        return metrics;
    }

    @Configuration
    @EnableAutoConfiguration(
            exclude = {
                    JndiConnectionFactoryAutoConfiguration.class,
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    JpaRepositoriesAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class
            })
    @ComponentScan(
            basePackages = {"com.sequenceiq.periscope", "com.sequenceiq.cloudbreak"},
            excludeFilters = {@Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    value = {
                            DatabaseConfig.class,
                            DatabaseMigrationConfig.class,
                            PeriscopeApplication.class,
                            MetricService.class
                    }
            ),
                    @Filter(type = FilterType.REGEX, pattern = "com.sequenceiq.periscope.modul.rejected.*")}
    )
    public static class TestConfig {

        @MockBean
        private CrudRepositoryLookupService repositoryLookupService;

        @MockBean
        private ClusterRepository clusterRepository;

        @MockBean
        private MetricAlertRepository metricAlertRepository;

        @MockBean
        private TimeAlertRepository timeAlertRepository;

        @MockBean
        private PrometheusAlertRepository prometheusAlertRepository;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private SecurityConfigRepository securityConfigRepository;

        @MockBean
        private ScalingPolicyRepository policyRepository;

        @MockBean
        private HistoryRepository historyRepository;

        @MockBean
        private PeriscopeNodeRepository periscopeNodeRepository;

        @MockBean
        private SubscriptionRepository subscriptionRepository;

        @MockBean
        private SwaggerResourcesProvider swaggerResourcesProvider;

        @MockBean
        private CachedUserDetailsService cachedUserDetailsService;

        @MockBean
        private Scheduler scheduler;

        @SpyBean
        private MetricService metricService;

        @Bean
        public EntityManagerFactory entityManagerFactory() {
            return mock(EntityManagerFactory.class);
        }

        @Bean
        public HikariDataSource hikariDataSource() {
            return mock(HikariDataSource.class);
        }

        @Bean(name = "jpaMappingContext")
        public JpaMetamodelMappingContext jpaMetamodelMappingContext() {
            return mock(JpaMetamodelMappingContext.class);
        }

        @Bean
        public JpaContext jpaContext() {
            return mock(JpaContext.class);
        }

        @Bean
        public TransactionExecutorService transactionExecutorService() {
            return new TransactionExecutionServiceTest();
        }

    }
}

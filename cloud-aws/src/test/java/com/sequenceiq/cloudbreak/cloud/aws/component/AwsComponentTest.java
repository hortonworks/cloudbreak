package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.component.AwsComponentTest.AwsTestContext;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.reactor.config.CloudReactorConfiguration;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.TemplateException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AwsTestContext.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
public abstract class AwsComponentTest {

    protected static final String AVAILABILITY_ZONE = "eu-west-1c";

    protected static final int SIZE_DISK_1 = 50;

    protected static final int SIZE_DISK_2 = 100;

    protected static final String INSTANCE_ID_1 = "i-0001";

    protected static final String INSTANCE_ID_2 = "i-0002";

    protected static final String INSTANCE_ID_3 = "i-0003";

    private static final String LOGIN_USER_NAME = "loginusername";

    private static final String PUBLIC_KEY = "pubkey";

    private static final int ROOT_VOLUME_SIZE = 50;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CIDR = "10.10.0.0/16";

    @Inject
    private freemarker.template.Configuration configuration;

    protected AuthenticatedContext getAuthenticatedContext() {
        Location location = location(region("region"), availabilityZone("availabilityZone"));
        CloudContext cloudContext = new CloudContext(1L, "cloudContextName", AWS, "variant", location, "owner@company.com", 5L);
        CloudCredential cloudCredential = new CloudCredential(3L, "credentialName");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    protected CloudStack getStack(InstanceStatus workerStatuses, InstanceStatus masterStatus) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        Security security = getSecurity();

        List<CloudInstance> masterInstances = List.of(
                getCloudInstance(instanceAuthentication, "master", masterStatus, 0L, null));

        List<CloudInstance> workerInstances = List.of(
                getCloudInstance(instanceAuthentication, "worker", workerStatuses, 0L, null),
                getCloudInstance(instanceAuthentication, "worker", workerStatuses, 1L, null),
                getCloudInstance(instanceAuthentication, "worker", InstanceStatus.STARTED, 2L, INSTANCE_ID_3));
        List<Group> groups = List.of(new Group("master", InstanceGroupType.CORE, masterInstances, security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE),
                new Group("worker", InstanceGroupType.CORE, workerInstances, security, null,
                        instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), Map.of(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    protected CloudStack getStackForLaunch(InstanceStatus createRequested, InstanceStatus createRequested1) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        CloudInstance instance = getCloudInstance(instanceAuthentication, "group1", InstanceStatus.CREATE_REQUESTED, 0L, null);
        Security security = getSecurity();

        List<Group> groups = List.of(new Group("group1", InstanceGroupType.CORE, List.of(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), Map.of(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    private Security getSecurity() {
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        return new Security(rules, emptyList());
    }

    private CloudInstance getCloudInstance(InstanceAuthentication instanceAuthentication, String groupName, InstanceStatus instanceStatus, long privateId,
            String instanceId) {
        List<Volume> volumes = Arrays.asList(
                new Volume("/hadoop/fs1", "HDD", SIZE_DISK_1),
                new Volume("/hadoop/fs2", "HDD", SIZE_DISK_2)
        );
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", groupName, privateId, volumes, instanceStatus,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25");
        Map<String, Object> params = new HashMap<>();
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, params);
    }

    @Configuration
    @ComponentScans({
            @ComponentScan(basePackages = {"com.sequenceiq.cloudbreak.cloud.aws", "com.sequenceiq.cloudbreak.cloud.template"}),
            @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.reactor.config", useDefaultFilters = false,
                    includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, value = CloudReactorConfiguration.class)),
    })
    public static class AwsTestContext {

        @MockBean(name = "DefaultRetryService")
        private Retry defaultRetryService;

        @MockBean
        private DefaultCostTaggingService defaultCostTaggingService;

        @MockBean
        private NetworkResourceBuilder<?> networkResourceBuilder;

        @MockBean
        private GroupResourceBuilder<?> groupResourceBuilder;

        @MockBean
        private ResourceNotifier resourceNotifier;

        @MockBean
        private AwsPlatformResources awsPlatformResources;

        @MockBean
        private AwsPlatformParameters awsPlatformParameters;

        @MockBean
        private AwsTagValidator awsTagValidator;

        @MockBean
        private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

        @MockBean
        private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

        @MockBean
        private AmazonEC2Client amazonEC2Client;

        @MockBean
        private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

        @Bean
        public AwsClient awsClient() {
            AwsClient awsClient = mock(AwsClient.class);
            when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
            when(awsClient.createAccess(any())).thenReturn(amazonEC2Client);
            when(awsClient.createCloudFormationRetryClient(any(), anyString())).thenReturn(amazonCloudFormationRetryClient);
            when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
            return awsClient;
        }

        @Bean
        public freemarker.template.Configuration configurationProvider() throws IOException, TemplateException {
            FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
            factoryBean.setPreferFileSystemAccess(false);
            factoryBean.setTemplateLoaderPath("classpath:/");
            factoryBean.afterPropertiesSet();
            return factoryBean.getObject();
        }

        @Bean
        public AsyncTaskExecutor resourceBuilderExecutor() {
            return new AsyncTaskExecutorTestImpl();
        }

        @Bean
        public AsyncTaskExecutor intermediateBuilderExecutor() {
            return new AsyncTaskExecutorTestImpl();
        }

        @Bean
        public SyncPollingScheduler<?> syncPollingScheduler() throws InterruptedException, ExecutionException, TimeoutException {
            SyncPollingScheduler<?> syncPollingScheduler = mock(SyncPollingScheduler.class);
            when(syncPollingScheduler.schedule(any())).thenAnswer(
                    getAnswer()
            );

            return syncPollingScheduler;
        }

        @Bean
        public CloudResourceHelper cloudResourceHelper() {
            return new CloudResourceHelper();
        }

        @Bean
        public AwsBackoffSyncPollingScheduler<?> awsBackoffSyncPollingScheduler() throws InterruptedException, ExecutionException, TimeoutException {
            AwsBackoffSyncPollingScheduler<?> awsBackoffSyncPollingScheduler = mock(AwsBackoffSyncPollingScheduler.class);
            when(awsBackoffSyncPollingScheduler.schedule(any())).thenAnswer(
                    getAnswer()
            );
            return awsBackoffSyncPollingScheduler;
        }

        static Answer<?> getAnswer() {
            return (Answer<?>) invocation -> {
                Object[] args = invocation.getArguments();
                Callable<?> task = (Callable<?>) args[0];
                return task.call();
            };
        }
    }
}

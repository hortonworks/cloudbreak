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
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.waiters.AmazonAutoScalingWaiters;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.component.AwsComponentTest.AwsTestContext;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
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
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerScheduledExecutor;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.tag.model.Tags;
import com.sequenceiq.common.api.type.InstanceGroupType;

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
        CloudCredential cloudCredential = new CloudCredential("crn", "credentialName");
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
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                        instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty()),
                new Group("worker", InstanceGroupType.CORE, workerInstances, security, null,
                        instanceAuthentication, instanceAuthentication.getLoginUserName(),
                        instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty()));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), new Tags(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    protected CloudStack getStackForLaunch(InstanceStatus createRequested, InstanceStatus createRequested1) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        CloudInstance instance = getCloudInstance(instanceAuthentication, "group1", InstanceStatus.CREATE_REQUESTED, 0L, null);
        Security security = getSecurity();

        List<Group> groups = List.of(new Group("group1", InstanceGroupType.CORE, List.of(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE, Optional.empty()));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), new Tags(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    private Security getSecurity() {
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        return new Security(rules, emptyList());
    }

    private CloudInstance getCloudInstance(InstanceAuthentication instanceAuthentication,
            String groupName, InstanceStatus instanceStatus, long privateId, String instanceId) {
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
    @ComponentScan(basePackages = {"com.sequenceiq.cloudbreak.cloud.aws", "com.sequenceiq.cloudbreak.cloud.template"})
    public static class AwsTestContext {

        @MockBean(name = "DefaultRetryService")
        private Retry defaultRetryService;

        @MockBean
        private CostTagging defaultCostTaggingService;

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
        private AmazonCloudFormationClient amazonCloudFormationClient;

        @MockBean
        private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

        @MockBean
        private AmazonEC2Client amazonEC2Client;

        @MockBean
        private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

        @MockBean
        private AmazonAutoScalingClient amazonAutoScalingClient;

        @MockBean
        private AmazonCloudFormationWaiters cfWaiters;

        @MockBean
        private AmazonAutoScalingWaiters asWaiters;

        @MockBean
        private AmazonEC2Waiters ecWaiters;

        @MockBean
        private Waiter<DescribeStacksRequest> cfStackWaiter;

        @MockBean
        private Waiter<DescribeInstancesRequest> instanceWaiter;

        @MockBean
        private Waiter<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequestWaiter;

        @Bean
        public CustomAmazonWaiterProvider customAmazonWaiterProvider() {
            CustomAmazonWaiterProvider provider = mock(CustomAmazonWaiterProvider.class);
            when(provider.getAutoscalingInstancesInServiceWaiter(any(), any())).thenReturn(describeAutoScalingGroupsRequestWaiter);

            return provider;
        }

        @Bean
        public AwsClient awsClient() {
            AwsClient awsClient = mock(AwsClient.class);
            when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
            when(awsClient.createAccess(any())).thenReturn(amazonEC2Client);
            when(awsClient.createCloudFormationRetryClient(any(), anyString())).thenReturn(amazonCloudFormationRetryClient);
            when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(amazonCloudFormationClient);
            when(amazonCloudFormationClient.waiters()).thenReturn(cfWaiters);
            when(cfWaiters.stackCreateComplete()).thenReturn(cfStackWaiter);
            when(cfWaiters.stackDeleteComplete()).thenReturn(cfStackWaiter);
            when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
            when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
            when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
            when(asWaiters.groupInService()).thenReturn(describeAutoScalingGroupsRequestWaiter);
            when(amazonEC2Client.waiters()).thenReturn(ecWaiters);
            when(ecWaiters.instanceRunning()).thenReturn(instanceWaiter);
            when(ecWaiters.instanceTerminated()).thenReturn(instanceWaiter);
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

        @Bean("reactorListeningScheduledExecutorService")
        public ListeningScheduledExecutorService reactorListeningScheduledExecutorService() {
            return MoreExecutors
                    .listeningDecorator(new MDCCleanerScheduledExecutor(40,
                            new ThreadFactoryBuilder().setNameFormat("cloud-api-%d").build()));
        }

        @Bean("cloudApiListeningScheduledExecutorService")
        public ListeningScheduledExecutorService cloudApiListeningScheduledExecutorService() {
            return MoreExecutors
                    .listeningDecorator(new MDCCleanerScheduledExecutor(40,
                            new ThreadFactoryBuilder().setNameFormat("cloud-api-%d").build()));
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
        public SyncPollingScheduler<?> syncPollingScheduler() throws Exception {
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

        static Answer<?> getAnswer() {
            return (Answer<?>) invocation -> {
                Object[] args = invocation.getArguments();
                Callable<?> task = (Callable<?>) args[0];
                return task.call();
            };
        }
    }
}

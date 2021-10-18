package com.sequenceiq.cloudbreak.cloud.handler.testcontext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.handler.ParameterGenerator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

import io.opentracing.Tracer;
import reactor.Environment;

@MockBeans({@MockBean(ApplicationFlowInformation.class), @MockBean(FlowLogDBService.class), @MockBean(FlowRegister.class)})
@Configuration
@ComponentScans({ @ComponentScan("com.sequenceiq.cloudbreak.cloud"), @ComponentScan("com.sequenceiq.flow.reactor"),
        @ComponentScan("com.sequenceiq.cloudbreak.auth"), @ComponentScan("com.sequenceiq.cloudbreak.client")})
@PropertySource("classpath:application.properties")
public class TestApplicationContext {

    private final InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

    private final CloudInstance cloudInstance = new CloudInstance("instanceId",
            new InstanceTemplate("flavor", "groupName", 1L, Collections.emptyList(),
                    InstanceStatus.CREATE_REQUESTED, new HashMap<>(), 0L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
            instanceAuthentication,
            "subnet-123",
            "az1");

    private final CloudInstance cloudInstanceBad = new CloudInstance("instanceIdBad",
            new InstanceTemplate("flavor", "groupName", 1L, Collections.emptyList(),
                    InstanceStatus.CREATE_REQUESTED, new HashMap<>(), 1L, "imageId", TemporaryStorage.ATTACHED_VOLUMES, 0L),
            instanceAuthentication,
            "subnet-123",
            "az1");

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private CredentialConnector credentialConnector;

    @Mock
    private MetadataCollector collector;

    @Mock
    private ResourceConnector<Object> resourceConnector;

    @Mock
    private InstanceConnector instanceConnector;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private Persister<?> persister;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private Tracer tracer;

    @Mock
    private ManagedChannelWrapper managedChannelWrapper;

    @Inject
    private ParameterGenerator g;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @PostConstruct
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Bean(name = "instance")
    public CloudInstance cloudInstance() {
        return cloudInstance;
    }

    @Bean(name = "bad-instance")
    public CloudInstance cloudInstanceBad() {
        return cloudInstanceBad;
    }

    @Bean
    public AltusDatabusConfiguration altusDatabusConfiguration() {
        return new AltusDatabusConfiguration("", false, "", "");
    }

    @Bean
    public ListeningScheduledExecutorService listeningScheduledExecutorService() {
        return MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(1));
    }

    @Bean
    public CloudPlatformConnectors cloudPlatformConnectors() {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        return cloudPlatformConnectors;
    }

    @Bean
    public Tracer tracer() {
        return tracer;
    }

    @Bean
    public CloudConnector cloudConnectors() throws Exception {
        CloudResource resource = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name("ref").build();
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudConnector.credentials()).thenReturn(credentialConnector);
        when(credentialConnector.create(any(AuthenticatedContext.class))).thenReturn(new CloudCredentialStatus(null, CredentialStatus.CREATED));
        when(credentialConnector.delete(any(AuthenticatedContext.class))).thenReturn(new CloudCredentialStatus(null, CredentialStatus.DELETED));
        when(authenticator.authenticate(any(), any())).thenReturn(g.createAuthenticatedContext());
        when(cloudConnector.platform()).thenReturn(Platform.platform("TESTCONNECTOR"));
        when(cloudConnector.variant()).thenReturn(Variant.variant("TESTVARIANT"));
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        when(cloudConnector.instances()).thenReturn(instanceConnector);
        when(cloudConnector.metadata()).thenReturn(collector);
        when(resourceConnector.launch(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudResourceStatus(resource, ResourceStatus.CREATED)));
        when(resourceConnector.terminate(any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudResourceStatus(resource, ResourceStatus.DELETED)));
        when(resourceConnector.update(any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudResourceStatus(resource, ResourceStatus.UPDATED)));
        when(resourceConnector.upscale(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudResourceStatus(resource, ResourceStatus.UPDATED)));
        when(resourceConnector.downscale(any(), any(), any(), anyList(), any()))
                .thenReturn(Collections.singletonList(new CloudResourceStatus(resource, ResourceStatus.UPDATED)));
        when(instanceConnector.check(any(), any()))
                .thenReturn(Collections.singletonList(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED)));
        CloudVmInstanceStatus collectInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS);
        when(collector.collect(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudVmMetaDataStatus(collectInstanceStatus, new CloudInstanceMetaData("privateIp", "publicIp",
                        "hypervisor"))));
        when(instanceConnector.start(any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED)));
        when(instanceConnector.stop(any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STOPPED)));
        when(instanceConnector.reboot(any(), any(), any()))
                .thenReturn(Collections.singletonList(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.STARTED)));
        when(instanceConnector.getConsoleOutput(any(), eq(cloudInstance)))
                .thenReturn(g.getSshFingerprint() + "    RSA/n-----END SSH HOST KEY FINGERPRINTS-----");
        when(instanceConnector.getConsoleOutput(any(), eq(cloudInstanceBad)))
                .thenReturn("XYZ    RSA/n-----END SSH HOST KEY FINGERPRINTS-----");
        return cloudConnector;
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public Persister<?> getPersister() {
        return persister;
    }

    @Bean
    public ResourceRetriever getResourceRetrieval() {
        return resourceRetriever;
    }

    @Bean
    public ManagedChannelWrapper managedChannelWrapper() {
        return managedChannelWrapper;
    }
}

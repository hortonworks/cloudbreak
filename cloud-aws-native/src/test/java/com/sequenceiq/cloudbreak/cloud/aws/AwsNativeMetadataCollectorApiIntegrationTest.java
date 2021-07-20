package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsDefaultZoneProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsEnvironmentVariableChecker;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSessionCredentialClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.config.AwsConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyMultiplePreferPrivate;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyMultiplePreferPublic;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsLifeCycleMapper;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

import io.opentracing.Tracer;

/*
    This is an integration test to check our API integration in case of missing resources during metadata collection.
    The properties `cb.aws.native.test.accesskey` and `cb.aws.native.test.secretkey` are required for operation.
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "cb.aws.hostkey.verify=true",
        "cb.vm.status.polling.interval=1",
        "cb.vm.status.polling.attempt=7",
        "cb.vm.retry.backoff.delay=20",
        "cb.vm.retry.backoff.multiplier=2",
        "cb.vm.retry.backoff.maxdelay=10000",
        "cb.vm.retry.attempt=5",
        "cb.aws.native.test.accesskey=",
        "cb.aws.native.test.secretkey="
})
@EnabledIf(expression = "#{environment['cb.aws.native.test.accesskey'] != '' && environment['cb.aws.native.test.secretkey'] != ''}", loadContext = true)
class AwsNativeMetadataCollectorApiIntegrationTest {

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private Tracer tracer;

    @MockBean
    private AwsPlatformResources awsPlatformResources;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Inject
    private AwsNativeMetadataCollector underTest;

    @Value("${cb.aws.native.test.accesskey}")
    private String awsAccessKey;

    @Value("${cb.aws.native.test.secretkey}")
    private String awsSecretKey;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-central-1")))
                .withUserId("user")
                .withAccountId("account")
                .build();
        Map<String, Object> credentialParams = Map.of("aws",
                Map.of("keyBased", Map.of("accessKey", awsAccessKey, "secretKey", awsSecretKey)));
        CloudCredential credential = new CloudCredential("id", "alma", credentialParams, false);
        authenticatedContext = awsAuthenticator.authenticate(context, credential);
    }

    @Test
    void collectInstanceMetadataWhenTheSpecifiedInstanceInstanceIdsDoNotExist() {
        InstanceTemplate instanceTemplate = new InstanceTemplate("flavor", "alma", 1L, Set.of(), InstanceStatus.UNKNOWN,
                Map.of(), 1L, "imageid", TemporaryStorage.ATTACHED_VOLUMES);
        CloudInstance cloudInstance1 = new CloudInstance("i-0000067a1cfd73843", instanceTemplate, null, "subnet-123", "az1", Map.of());
        CloudInstance cloudInstance2 = new CloudInstance("i-0000057a1cfd73843", instanceTemplate, null, "subnet-123", "az1", Map.of());
        List<CloudInstance> vms = List.of(cloudInstance1, cloudInstance2);

        List<CloudVmMetaDataStatus> collect = underTest.collect(authenticatedContext, List.of(), vms, vms);

        assertTrue(collect.isEmpty());
    }

    @Test
    void collectLoadBalancerMetadataWhenTheSpecifiedArnsDoNotExist() {
        List<LoadBalancerType> loadbalancerTypes = List.of();
        CloudResource aLoadBalancer = new CloudResource.Builder()
                .name("aLoadBalancer")
                .reference("arn:aws:elasticloadbalancing:eu-central-1:152813717728:loadbalancer/net/notexisting-LoadB-1MYWS0111IT2K/2e65a6e6b1a801df")
                .type(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        CloudResource secondLoadBalancer = new CloudResource.Builder()
                .name("secondLoadBalancer")
                .reference("arn:aws:elasticloadbalancing:eu-central-1:152813717728:loadbalancer/net/notexisting2-LoadB-1MYWS0111IT2K/2e65a6e6b1a801bf")
                .type(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        List<CloudResource> resources = List.of(aLoadBalancer, secondLoadBalancer);

        List<CloudLoadBalancerMetadata> cloudLoadBalancerMetadata = underTest.collectLoadBalancer(authenticatedContext, loadbalancerTypes, resources);

        assertTrue(cloudLoadBalancerMetadata.isEmpty());
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.aws.metadata")
    @Import({
            AwsTagValidator.class,
            AwsPlatformParameters.class,
            CloudbreakResourceReaderService.class,
            AwsAuthenticator.class,
            CommonAwsClient.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class,
            LoadBalancerTypeConverter.class,
            SdkClientExceptionMapper.class,
            AwsLifeCycleMapper.class,
            SubnetFilterStrategyMultiplePreferPublic.class,
            SubnetFilterStrategyMultiplePreferPrivate.class,
            SubnetSelectorService.class,
            AwsEncodedAuthorizationFailureMessageDecoder.class,
            AwsConfig.class,
    })
    static class AwsNativeMetadataCollectorTestConfig {

    }
}
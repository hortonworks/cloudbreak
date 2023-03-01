package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.common.type.TemporaryStorage.ATTACHED_VOLUMES;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsDefaultZoneProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsEnvironmentVariableChecker;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSessionCredentialClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.config.AwsConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsRegionEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsServiceEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyMultiplePreferPrivate;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyMultiplePreferPublic;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "cb.max.aws.resource.name.length=5")
@Import(SdkClientExceptionMapper.class)
class AwsValidatorsTest {

    public static final String EMPTY = "";

    public static final String VALID = "sheep";

    @Inject
    private AwsTagValidator awsTagValidatorUnderTest;

    @Inject
    private AwsStackValidator awsStackValidatorUnderTest;

    @Mock
    private AwsEndpointProvider awsEndpointProvider;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @SpyBean
    private AwsCloudFormationClient awsClient;

    @MockBean
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    @MockBean
    private AwsPlatformResources awsPlatformResources;

    @MockBean
    private AwsPageCollector awsPageCollector;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void prepare() {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("stackName")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az")))
                .withAccountId("account")
                .build();
        CloudCredential cloudCredential = new CloudCredential(null, null, Map.of("accessKey", "accessKey", "secretKey", "secretKey"), null);
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(any(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        authenticatedContext = new AuthenticatedContext(context, cloudCredential);
    }

    @Test
    void testStackValidatorStackAlreadyExist() {
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(), anyString());
        assertThrows(CloudConnectorException.class, () -> awsStackValidatorUnderTest.validate(authenticatedContext, null));
    }

    @Test
    void testStackValidatorStackUnexistent() {
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(), anyString());
        when(amazonCloudFormationClient.describeStacks(any()))
                .thenThrow(AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("stackName does not exist").build()).build());
        InstanceTemplate template =
                new InstanceTemplate("noStorage", "worker", 0L, List.of(), InstanceStatus.CREATE_REQUESTED, Map.of(), 0L, "", ATTACHED_VOLUMES, 0L);
        CloudInstance instance = new CloudInstance("", template, null, "subnet-1", "az1");
        Group group = new Group("worker", InstanceGroupType.CORE, List.of(instance), null, null, null, "", "", 0, Optional.empty(), createGroupNetwork(),
                emptyMap());
        CloudStack cloudStack = new CloudStack(List.of(group), null, null, Map.of(), Map.of(), "", null, "", "", null);
        awsStackValidatorUnderTest.validate(authenticatedContext, cloudStack);
    }

    @Test
    void testStackValidatorStackUseRetryClient() {
        CloudFormationClient client = mock(CloudFormationClient.class);
        doReturn(client).when(awsClient).createCloudFormation(any(), anyString());
        when(client.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(RetryableException.builder().message("repeat1 Rate exceeded").build())
                .thenThrow(RetryableException.builder().message("repeat2 Request limit exceeded").build())
                .thenReturn(null);
        assertThrows(CloudConnectorException.class, () -> awsStackValidatorUnderTest.validate(authenticatedContext, null));
        verify(client, times(3)).describeStacks(any(DescribeStacksRequest.class));
    }

    @TestFactory
    Collection<DynamicTest> testCheckStatuses() {
        ArrayList<DynamicTest> tests = new ArrayList<>();
        tests.add(DynamicTest.dynamicTest("tag is too short", () -> testTagsWithExpectedException(EMPTY, VALID)));
        tests.add(DynamicTest.dynamicTest("tag has leading white space", () -> testTagsWithExpectedException(' ' + VALID, VALID)));
        tests.add(DynamicTest.dynamicTest("tag has trailing white space", () -> testTagsWithExpectedException(VALID + ' ', VALID)));
        tests.add(DynamicTest.dynamicTest("tag is too long", () -> testTagsWithExpectedException(testStringWithLength(128), VALID)));
        tests.add(DynamicTest.dynamicTest("tag is valid but long",
                () -> testTagsWithExpectedTeBeFair(testStringWithLength(127), VALID)));
        tests.add(DynamicTest.dynamicTest("tag is valid and short",
                () -> testTagsWithExpectedTeBeFair(testStringWithLength(1), VALID)));
        tests.add(DynamicTest.dynamicTest("tag is valid strange chars", () -> testTagsWithExpectedTeBeFair("+-=._:/@", VALID)));
        tests.add(DynamicTest.dynamicTest("tag is valid numeric chars", () -> testTagsWithExpectedTeBeFair("1234567890", VALID)));
        tests.add(DynamicTest.dynamicTest("tag is valid alphabetic chars",
                () -> testTagsWithExpectedTeBeFair("A aB bC cD dE eF fG gH hI iJ jK kL lM mN nO oP pQ qR rS sT tU uV vW wX xY yZ z", VALID)));
        tests.add(DynamicTest.dynamicTest("tag should not start with aws", () -> testTagsWithExpectedException("aws1234567890", VALID)));

        tests.add(DynamicTest.dynamicTest("value is too short", () -> testTagsWithExpectedException(VALID, EMPTY)));
        tests.add(DynamicTest.dynamicTest("value has leading whitespace", () -> testTagsWithExpectedException(VALID, ' ' + VALID)));
        tests.add(DynamicTest.dynamicTest("value has trailing whitespace", () -> testTagsWithExpectedException(VALID, VALID + ' ')));
        tests.add(DynamicTest.dynamicTest("value is too long", () -> testTagsWithExpectedException(VALID, testStringWithLength(256))));
        tests.add(DynamicTest.dynamicTest("value is valid but long",
                () -> testTagsWithExpectedTeBeFair(VALID, testStringWithLength(255))));
        tests.add(DynamicTest.dynamicTest("value is valid and short",
                () -> testTagsWithExpectedTeBeFair(VALID, testStringWithLength(1))));
        tests.add(DynamicTest.dynamicTest("value is valid strange chars", () -> testTagsWithExpectedTeBeFair(VALID, "+-=._:/@")));
        tests.add(DynamicTest.dynamicTest("value is valid numeric chars", () -> testTagsWithExpectedTeBeFair(VALID, "1234567890")));
        tests.add(DynamicTest.dynamicTest("value is valid alphabetic chars",
                () -> testTagsWithExpectedTeBeFair("sheep", "A aB bC cD dE eF fG gH hI iJ jK kL lM mN nO oP pQ qR rS sT tU uV vW wX xY yZ z")));
        tests.add(DynamicTest.dynamicTest("value could start with aws", () -> testTagsWithExpectedTeBeFair(VALID, "aws1234567890")));

        tests.add(DynamicTest.dynamicTest("too many tags",
                () -> assertThrows(IllegalArgumentException.class,
                        () -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(getManyTags(51))))));
        tests.add(DynamicTest.dynamicTest("so many tags but valid",
                () -> assertDoesNotThrow(() -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(getManyTags(50))))));
        tests.add(DynamicTest.dynamicTest("no tags at all",
                () -> assertDoesNotThrow(() -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(getManyTags(0))))));

        return tests;
    }

    private String testStringWithLength(int stringLength) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, stringLength).forEach(i -> sb.append("x"));
        return sb.toString();
    }

    private void testTagsWithExpectedException(String key, String value) {
        Map<String, String> tags = new HashMap<>();
        tags.put(key, value);
        assertThrows(IllegalArgumentException.class, () -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(tags)));
    }

    private void testTagsWithExpectedTeBeFair(String key, String value) {
        Map<String, String> tags = new HashMap<>();
        tags.put(key, value);
        assertDoesNotThrow(() -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(tags)));
    }

    private Map<String, String> getManyTags(int numberOfTags) {
        Map<String, String> tags = new HashMap<>();
        IntStream.range(0, numberOfTags).forEach(i -> tags.put(Integer.toString(i), VALID));
        return tags;
    }

    private CloudStack getTestCloudStackWithTags(Map<String, String> tags) {
        return new CloudStack(List.of(), null, null, Map.of(), tags, "", null, null, null, null);
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector")
    @Import({ AwsTagValidator.class,
            AwsPlatformParameters.class,
            CloudbreakResourceReaderService.class,
            AwsConfig.class,
            AwsAuthenticator.class,
            CommonAwsClient.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class,
            AwsStackValidator.class,
            CloudFormationStackUtil.class,
            LoadBalancerTypeConverter.class,
            SdkClientExceptionMapper.class,
            SubnetFilterStrategyMultiplePreferPublic.class,
            SubnetFilterStrategyMultiplePreferPrivate.class,
            SubnetSelectorService.class,
            AwsEndpointProvider.class,
            AwsRegionEndpointProvider.class,
            AwsServiceEndpointProvider.class,
    })
    static class Config {

    }
}

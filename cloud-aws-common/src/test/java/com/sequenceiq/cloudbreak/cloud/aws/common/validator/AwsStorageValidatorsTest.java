package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static com.sequenceiq.cloudbreak.common.type.TemporaryStorage.EPHEMERAL_VOLUMES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsDefaultZoneProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsEnvironmentVariableChecker;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSessionCredentialClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.config.AwsConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsRegionEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsServiceEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricPublisher;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(SpringExtension.class)
    @TestPropertySource(properties = "cb.max.aws.resource.name.length=5")
@Import(SdkClientExceptionMapper.class)
public class AwsStorageValidatorsTest {

    public static final String EMPTY = "";

    public static final String VALID = "sheep";

    @Inject
    private AwsTagValidator awsTagValidatorUnderTest;

    @Inject
    private AwsStorageValidator awsStorageValidatorUnderTest;

    @MockBean
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    @MockBean
    private AwsPlatformResources awsPlatformResources;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private AwsMetricPublisher awsMetricPublisher;

    @SpyBean
    private AwsApacheClient awsApacheClient;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    public void prepare() {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("stackName")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az")))
                .withAccountId("account")
                .build();
        CloudCredential cloudCredential = new CloudCredential();
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(any(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        authenticatedContext = new AuthenticatedContext(context, cloudCredential);
    }

    @Test
    public void testStackValidatorNoInstanceStorage() {
        Volume volume = new Volume("SSD", "SSD", 100, CloudVolumeUsageType.GENERAL);
        InstanceTemplate noStorageTemplate =
                new InstanceTemplate("noStorage", "worker", 0L, List.of(volume), InstanceStatus.CREATE_REQUESTED, Map.of(), 0L, "", EPHEMERAL_VOLUMES, 0L);
        InstanceTemplate storageTemplate =
                new InstanceTemplate("storage", "compute", 0L, List.of(volume), InstanceStatus.CREATE_REQUESTED, Map.of(), 0L, "", EPHEMERAL_VOLUMES, 0L);
        CloudInstance noStorageInstance = new CloudInstance("", noStorageTemplate, null, "subnet-1", "az1");
        CloudInstance storageInstance = new CloudInstance("", storageTemplate, null, "subnet-1", "az1");
        Group noStorageGroup = Group.builder()
                .withInstances(List.of(noStorageInstance))
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build();
        Group storageGroup = Group.builder()
                .withInstances(List.of(storageInstance))
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build();
        CloudStack cloudStack = CloudStack.builder()
                .groups(List.of(noStorageGroup, storageGroup))
                .build();
        CloudVmTypes cloudVmTypes = new CloudVmTypes();
        VmType storageType = VmType.vmTypeWithMeta("storage", VmTypeMeta.VmTypeMetaBuilder.builder()
                .withEphemeralConfig(new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1))
                .create(), true);
        VmType noStorageType = VmType.vmTypeWithMeta("noStorage", VmTypeMeta.VmTypeMetaBuilder.builder().create(), true);
        Map<String, Set<VmType>> responses = Map.of("az", Set.of(storageType, noStorageType));
        cloudVmTypes.setCloudVmResponses(responses);
        when(awsPlatformResources.virtualMachines(any(), eq(Region.region("region")), any())).thenReturn(cloudVmTypes);
        when(entitlementService.getEntitlements(any())).thenReturn(new ArrayList<>());
        assertThrows(CloudConnectorException.class,
                () -> awsStorageValidatorUnderTest.validate(authenticatedContext, cloudStack),
                "The following instance types does not support instance storage: [noStorage]");
    }

    @TestFactory
    public Collection<DynamicTest> testCheckStatuses() {
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
        tests.add(DynamicTest.dynamicTest("value is valid alphaAwsLogRolePermissionValidatorbetic chars",
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
        return CloudStack.builder()
                .tags(tags)
                .build();
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector")
    @Import({AwsTagValidator.class,
            AwsPlatformParameters.class,
            CloudbreakResourceReaderService.class,
            AwsConfig.class,
            AwsAuthenticator.class,
            CommonAwsClient.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class,
            AwsStorageValidator.class,
            LoadBalancerTypeConverter.class,
            SdkClientExceptionMapper.class,
            SubnetFilterStrategyMultiplePreferPublic.class,
            SubnetFilterStrategyMultiplePreferPrivate.class,
            SubnetSelectorService.class,
            AwsPageCollector.class,
            AwsRegionEndpointProvider.class,
            AwsServiceEndpointProvider.class
    })
    static class Config {

    }
}

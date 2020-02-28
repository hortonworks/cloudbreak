package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.conf.AwsConfig;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.RetryService;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "cb.max.aws.resource.name.length=5")
public class AwsValidatorsTest {

    public static final String EMPTY = "";

    public static final String VALID = "sheep";

    @Inject
    private AwsTagValidator awsTagValidatorUnderTest;

    @Inject
    private AwsStackValidator awsStackValidatorUnderTest;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Mock
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @Mock
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @SpyBean
    private AwsClient awsClient;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    public void prepare() {
        CloudContext cloudContext = new CloudContext(1L, "stackName", "AWS", "AWS", Location.location(Region.region("region")), "user", "account");
        CloudCredential cloudCredential = null;
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);

    }

    @Test
    public void testStackValidatorStackAlreadyExist() {
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(), anyString());
        Assertions.assertThrows(CloudConnectorException.class, () -> awsStackValidatorUnderTest.validate(authenticatedContext, null));
    }

    @Test
    public void testStackValidatorStackUnexistent() {
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(), anyString());
        when(amazonCloudFormationRetryClient.describeStacks(any())).thenThrow(new AmazonServiceException("test exist"));
        Assertions.assertDoesNotThrow(() -> awsStackValidatorUnderTest.validate(authenticatedContext, null));
    }

    @Test
    public void testStackValidatorStackUseRetryClient() {
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(), anyString());
        when(amazonCloudFormationClient.describeStacks(any()))
                .thenThrow(new SdkClientException("repeat1 Rate exceeded"))
                .thenThrow(new SdkClientException("repeat2Request limit exceeded"))
                .thenReturn(null);
        Assertions.assertThrows(CloudConnectorException.class, () -> awsStackValidatorUnderTest.validate(authenticatedContext, null));
        verify(amazonCloudFormationClient, times(3)).describeStacks(any());
    }

    @TestFactory
    public Collection<DynamicTest> testCheckStatuses() {
        ArrayList<DynamicTest> tests = new ArrayList<>();
        tests.add(DynamicTest.dynamicTest("tag is too short", () -> testTagsWithExpectedException(EMPTY, VALID)));
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
        tests.add(DynamicTest.dynamicTest("value is too long", () -> testTagsWithExpectedException(VALID, testStringWithLength(256))));
        tests.add(DynamicTest.dynamicTest("value is valid but long",
                () -> testTagsWithExpectedTeBeFair(VALID, testStringWithLength(255))));
        tests.add(DynamicTest.dynamicTest("value is valid and short",
                () -> testTagsWithExpectedTeBeFair(VALID, testStringWithLength(1))));
        tests.add(DynamicTest.dynamicTest("value is valid strange chars", () -> testTagsWithExpectedTeBeFair(VALID, "+-=._:/@")));
        tests.add(DynamicTest.dynamicTest("value is valid numeric chars", () -> testTagsWithExpectedTeBeFair(VALID, "1234567890")));
        tests.add(DynamicTest.dynamicTest("value is valid alphabetic chars",
                () -> testTagsWithExpectedTeBeFair("sheep", "A aB bC cD dE eF fG gH hI iJ jK kL lM mN nO oP pQ qR rS sT tU uV vW wX xY yZ z")));
        tests.add(DynamicTest.dynamicTest("value should not start with aws", () -> testTagsWithExpectedException(VALID, "aws1234567890")));

        tests.add(DynamicTest.dynamicTest("too many tags",
                () -> Assertions.assertThrows(IllegalArgumentException.class,
                        () -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(getManyTags(51))))));
        tests.add(DynamicTest.dynamicTest("so many tags but valid",
                () -> Assertions.assertDoesNotThrow(() -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(getManyTags(50))))));
        tests.add(DynamicTest.dynamicTest("no tags at all",
                () -> Assertions.assertDoesNotThrow(() -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(getManyTags(0))))));

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
        Assertions.assertThrows(IllegalArgumentException.class, () -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(tags)));
    }

    private void testTagsWithExpectedTeBeFair(String key, String value) {
        Map<String, String> tags = new HashMap<>();
        tags.put(key, value);
        Assertions.assertDoesNotThrow(() -> awsTagValidatorUnderTest.validate(authenticatedContext, getTestCloudStackWithTags(tags)));
    }

    private Map<String, String> getManyTags(int numberOfTags) {
        Map<String, String> tags = new HashMap<>();
        IntStream.range(0, numberOfTags).forEach(i -> tags.put(Integer.toString(i), VALID));
        return tags;
    }

    private CloudStack getTestCloudStackWithTags(Map<String, String> tags) {
        return new CloudStack(List.of(), null, null, Map.of(), tags,
                "", null, null, null, null);
    }

    @Configuration
    @EnableRetry(proxyTargetClass = true)
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector")
    @Import({AwsTagValidator.class,
            AwsPlatformParameters.class,
            CloudbreakResourceReaderService.class,
            AwsConfig.class,
            AwsAuthenticator.class,
            AwsClient.class,
            AwsSessionCredentialClient.class,
            AwsDefaultZoneProvider.class,
            AwsEnvironmentVariableChecker.class,
            RetryService.class,
            AwsStackValidator.class,
            CloudFormationStackUtil.class
    })
    static class Config {

    }
}

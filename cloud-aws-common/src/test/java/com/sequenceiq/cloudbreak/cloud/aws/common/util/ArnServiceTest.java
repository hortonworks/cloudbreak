package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ArnServiceTest {

    private static final String ARN_INSTANCE_PROFILE = "arn:aws-us-gov:iam::123456789012:instance-profile/my-profile";

    private static final String ARN_ROLE = "arn:aws-us-gov:iam::123456789012:role/my-role";

    private static final String ARN_USER = "arn:aws-us-gov:iam::123456789012:user/my-user";

    private static final String ARN_EC2_INSTANCE = "arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-0bc43096314295350";

    private static final String ARN_SECRETSMANAGER_SECRET = "arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-rk4Dy0";

    private ArnService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ArnService();
    }

    static Object[][] isInstanceProfileArnTestDataProvider() {
        return new Object[][]{
                // resourceArn, responseExpected
                {ARN_EC2_INSTANCE, false},
                {ARN_SECRETSMANAGER_SECRET, false},
                {ARN_ROLE, false},
                {ARN_USER, false},
                {ARN_INSTANCE_PROFILE, true},
        };
    }

    @ParameterizedTest(name = "resourceArn={0}")
    @MethodSource("isInstanceProfileArnTestDataProvider")
    void isInstanceProfileArnTest(String resourceArn, boolean responseExpected) {
        assertThat(underTest.isInstanceProfileArn(resourceArn)).isEqualTo(responseExpected);
    }

    static Object[][] isRoleProfileArnTestDataProvider() {
        return new Object[][]{
                // resourceArn, responseExpected
                {ARN_EC2_INSTANCE, false},
                {ARN_SECRETSMANAGER_SECRET, false},
                {ARN_ROLE, true},
                {ARN_USER, false},
                {ARN_INSTANCE_PROFILE, false},
        };
    }

    @ParameterizedTest(name = "resourceArn={0}")
    @MethodSource("isRoleProfileArnTestDataProvider")
    void isRoleProfileArnTest(String resourceArn, boolean responseExpected) {
        assertThat(underTest.isRoleArn(resourceArn)).isEqualTo(responseExpected);
    }

    static Object[][] isEc2InstanceArnTestDataProvider() {
        return new Object[][]{
                // resourceArn, responseExpected
                {ARN_EC2_INSTANCE, true},
                {ARN_SECRETSMANAGER_SECRET, false},
                {ARN_ROLE, false},
                {ARN_USER, false},
                {ARN_INSTANCE_PROFILE, false},
        };
    }

    @ParameterizedTest(name = "resourceArn={0}")
    @MethodSource("isEc2InstanceArnTestDataProvider")
    void isEc2InstanceArnTest(String resourceArn, boolean responseExpected) {
        assertThat(underTest.isEc2InstanceArn(resourceArn)).isEqualTo(responseExpected);
    }

    static Object[][] isSecretsManagerSecretArnTestDataProvider() {
        return new Object[][]{
                // resourceArn, responseExpected
                {ARN_EC2_INSTANCE, false},
                {ARN_SECRETSMANAGER_SECRET, true},
                {ARN_ROLE, false},
                {ARN_USER, false},
                {ARN_INSTANCE_PROFILE, false},
        };
    }

    @ParameterizedTest(name = "resourceArn={0}")
    @MethodSource("isSecretsManagerSecretArnTestDataProvider")
    void isSecretsManagerSecretArnTest(String resourceArn, boolean responseExpected) {
        assertThat(underTest.isSecretsManagerSecretArn(resourceArn)).isEqualTo(responseExpected);
    }

}
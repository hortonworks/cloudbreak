package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialSettings;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.OrganizationsDecisionDetail;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
public class AwsCredentialVerifierTest {

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private CommonAwsClient awsClient;

    @InjectMocks
    private AwsCredentialVerifier awsCredentialVerifier;

    @Test
    public void verifyCredentialAndThrowFailExceptionTest() throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, "acc", new CloudCredentialSettings(true, false));

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResponse getCallerIdentityResult = GetCallerIdentityResponse.builder().arn("arn").build();
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("denied_action1_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("denied_action2_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("denied_action3_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("accepted_action_" + i).evalResourceName("*").build());
            SimulatePrincipalPolicyResponse simulatePrincipalPolicyResult = SimulatePrincipalPolicyResponse.builder()
                    .evaluationResults(evaluationResults).build();
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential), encodedAwsEnvPolicy);
            fail("It shoud throw verification exception");
        } catch (AwsPermissionMissingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3"));
            assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals(simulateRequestNumber, allSimulatePrincipalPolicyRequest.size(),
                "expect if " + simulateRequestNumber + " simulate request has been sent");
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.policySourceArn()));

    }

    @Test
    public void verifyCredentialTest() throws IOException, AwsPermissionMissingException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, "acc", new CloudCredentialSettings(true, false));

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResponse getCallerIdentityResult = GetCallerIdentityResponse.builder()
                .arn("arn").build();
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
        evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalActionName("accepted_action1").evalResourceName("aws:ec2").build());
        evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalActionName("accepted_action2").evalResourceName("aws:ec2").build());
        evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalActionName("accepted_action3").evalResourceName("aws:ec2").build());
        evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .evalActionName("accepted_action4").evalResourceName("*").build());
        SimulatePrincipalPolicyResponse simulatePrincipalPolicyResult = SimulatePrincipalPolicyResponse.builder()
                .evaluationResults(evaluationResults).build();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenReturn(simulatePrincipalPolicyResult);

        awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential), encodedAwsEnvPolicy);
    }

    @Test
    public void verifyCredentialAndThrowFailExceptionBecauseOrganizatioRuleTest() throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, "acc", new CloudCredentialSettings(true, false));

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResponse getCallerIdentityResult = GetCallerIdentityResponse.builder()
                .arn("arn").build();
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("denied_action1_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                    .evalActionName("denied_action2_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                    .evalActionName("denied_action3_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("accepted_action_" + i).evalResourceName("*").build());
            SimulatePrincipalPolicyResponse simulatePrincipalPolicyResult = SimulatePrincipalPolicyResponse.builder()
                    .evaluationResults(evaluationResults).build();
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential), encodedAwsEnvPolicy);
            fail("It shoud throw verification exception");
        } catch (AwsPermissionMissingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1_0 : aws:ec2,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2_0 : aws:ec2 -> Denied by Organization Rule,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3_0 : aws:ec2 -> Denied by Organization Rule,"));
            assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals(simulateRequestNumber, allSimulatePrincipalPolicyRequest.size(),
                "expect if " + simulateRequestNumber + " simulate request has been sent");
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.policySourceArn()));
    }

    @Test
    public void verifyCredentialAndSkipOrganizationErrorsWhenSkipOrgPolicyDecisionsIsTrue() throws IOException, AwsPermissionMissingException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, "acc", new CloudCredentialSettings(true, true));

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResponse getCallerIdentityResult = GetCallerIdentityResponse.builder()
                .arn("arn").build();
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                    .evalActionName("denied_action2_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                    .evalActionName("denied_action3_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                    .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                    .evalActionName("accepted_action_" + i).evalResourceName("*").build());
            SimulatePrincipalPolicyResponse simulatePrincipalPolicyResult = SimulatePrincipalPolicyResponse.builder()
                    .evaluationResults(evaluationResults).build();
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential), encodedAwsEnvPolicy);
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals(simulateRequestNumber, allSimulatePrincipalPolicyRequest.size(),
                "expect if " + simulateRequestNumber + " simulate request has been sent");
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.policySourceArn()));
    }

    @Test
    public void verifyCredentialAndOrganizatioDecisionDetailIsNullTest() throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        String encodedAwsEnvPolicy = Base64Util.encode(awsEnvPolicy);
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, "acc", new CloudCredentialSettings(true, false));

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResponse getCallerIdentityResult = GetCallerIdentityResponse.builder()
                .arn("arn").build();
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail((OrganizationsDecisionDetail) null)
                    .evalActionName("denied_action1_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail((OrganizationsDecisionDetail) null)
                    .evalActionName("denied_action2_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("deny")
                    .organizationsDecisionDetail((OrganizationsDecisionDetail) null)
                    .evalActionName("denied_action3_" + i).evalResourceName("aws:ec2").build());
            evaluationResults.add(EvaluationResult.builder().evalDecision("accept")
                    .organizationsDecisionDetail((OrganizationsDecisionDetail) null)
                    .evalActionName("accepted_action_" + i).evalResourceName("*").build());
            SimulatePrincipalPolicyResponse simulatePrincipalPolicyResult = SimulatePrincipalPolicyResponse.builder()
                    .evaluationResults(evaluationResults).build();
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential), encodedAwsEnvPolicy);
            fail("It shoud throw verification exception");
        } catch (AwsPermissionMissingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1_0 : aws:ec2,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2_0 : aws:ec2,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3_0 : aws:ec2,"));
            assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals(simulateRequestNumber, allSimulatePrincipalPolicyRequest.size(),
                "expect if " + simulateRequestNumber + " simulate request has been sent");
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.policySourceArn()));

    }
}

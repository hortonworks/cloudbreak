package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.OrganizationsDecisionDetail;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@RunWith(MockitoJUnitRunner.class)
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
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes()));
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, false);

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("arn");
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = new SimulatePrincipalPolicyResult();
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("denied_action1_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("denied_action2_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("denied_action3_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("accepted_action_" + i).withEvalResourceName("*"));
            simulatePrincipalPolicyResult.setEvaluationResults(evaluationResults);
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential));
            fail("It shoud throw verification exception");
        } catch (AwsPermissionMissingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3"));
            assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals("expect if " + simulateRequestNumber + " simulate request has been sent",
                simulateRequestNumber, allSimulatePrincipalPolicyRequest.size());
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.getPolicySourceArn()));

    }

    @Test
    public void verifyCredentialTest() throws IOException, AwsPermissionMissingException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes()));
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, false);

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("arn");
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = new SimulatePrincipalPolicyResult();
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("accepted_action1").withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("accepted_action2").withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("accepted_action3").withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("accepted_action4").withEvalResourceName("*"));
            simulatePrincipalPolicyResult.setEvaluationResults(evaluationResults);
            when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenReturn(simulatePrincipalPolicyResult);

        awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential));
    }

    @Test
    public void verifyCredentialAndThrowFailExceptionBecauseOrganizatioRuleTest() throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes()));
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, false);

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("arn");
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = new SimulatePrincipalPolicyResult();
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("denied_action1_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(false))
                    .withEvalActionName("denied_action2_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(false))
                    .withEvalActionName("denied_action3_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(new OrganizationsDecisionDetail().withAllowedByOrganizations(true))
                    .withEvalActionName("accepted_action_" + i).withEvalResourceName("*"));
            simulatePrincipalPolicyResult.setEvaluationResults(evaluationResults);
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential));
            fail("It shoud throw verification exception");
        } catch (AwsPermissionMissingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1_0 : aws:ec2,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2_0 : aws:ec2 -> Denied by Organization Rule,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3_0 : aws:ec2 -> Denied by Organization Rule,"));
            assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals("expect if " + simulateRequestNumber + " simulate request has been sent",
                simulateRequestNumber, allSimulatePrincipalPolicyRequest.size());
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.getPolicySourceArn()));

    }

    @Test
    public void verifyCredentialAndOrganizatioDecisionDetailIsNullTest() throws IOException {
        URL url = Resources.getResource("definitions/aws-environment-minimal-policy.json");
        String awsEnvPolicy = Resources.toString(url, Charsets.UTF_8);
        when(awsPlatformParameters.getEnvironmentMinimalPoliciesJson()).thenReturn(Base64.getEncoder().encodeToString(awsEnvPolicy.getBytes()));
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters, false);

        AmazonIdentityManagementClient amazonIdentityManagement = mock(AmazonIdentityManagementClient.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AmazonSecurityTokenServiceClient awsSecurityTokenService = mock(AmazonSecurityTokenServiceClient.class);
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("arn");
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = new SimulatePrincipalPolicyResult();
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(null)
                    .withEvalActionName("denied_action1_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(null)
                    .withEvalActionName("denied_action2_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withOrganizationsDecisionDetail(null)
                    .withEvalActionName("denied_action3_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withOrganizationsDecisionDetail(null)
                    .withEvalActionName("accepted_action_" + i).withEvalResourceName("*"));
            simulatePrincipalPolicyResult.setEvaluationResults(evaluationResults);
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential));
            fail("It shoud throw verification exception");
        } catch (AwsPermissionMissingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1_0 : aws:ec2,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2_0 : aws:ec2,"));
            assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3_0 : aws:ec2,"));
            assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 5;
        assertEquals("expect if " + simulateRequestNumber + " simulate request has been sent",
                simulateRequestNumber, allSimulatePrincipalPolicyRequest.size());
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.getPolicySourceArn()));

    }
}

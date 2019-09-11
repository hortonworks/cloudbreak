package com.sequenceiq.cloudbreak.cloud.aws;

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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@RunWith(MockitoJUnitRunner.class)
public class AwsCredentialVerifierTest {

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private AwsClient awsClient;

    @InjectMocks
    private AwsCredentialVerifier awsCredentialVerifier;

    @Test
    public void verifyCredentialAndThrowFailExceptionTest() throws IOException {
        URL url = Resources.getResource("definitions/aws-cb-policy.json");
        String awsCbPolicy = Resources.toString(url, Charsets.UTF_8);
        when(awsPlatformParameters.getCredentialPoliciesJson()).thenReturn(Base64.getEncoder().encodeToString(awsCbPolicy.getBytes()));
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters);

        AmazonIdentityManagement amazonIdentityManagement = mock(AmazonIdentityManagement.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AWSSecurityTokenService awsSecurityTokenService = mock(AWSSecurityTokenService.class);
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("arn");
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createAwsSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        AtomicInteger i = new AtomicInteger();
        when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenAnswer(invocation -> {
            SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = new SimulatePrincipalPolicyResult();
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withEvalActionName("denied_action1_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withEvalActionName("denied_action2_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("deny")
                    .withEvalActionName("denied_action3_" + i).withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withEvalActionName("accepted_action_" + i).withEvalResourceName("*"));
            simulatePrincipalPolicyResult.setEvaluationResults(evaluationResults);
            i.getAndIncrement();
            return simulatePrincipalPolicyResult;
        });

        try {
            awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential));
            fail("It shoud throw verification exception");
        } catch (AwsCredentialVerificationException e) {
            Assert.assertThat(e.getMessage(), CoreMatchers.containsString("denied_action1"));
            Assert.assertThat(e.getMessage(), CoreMatchers.containsString("denied_action2"));
            Assert.assertThat(e.getMessage(), CoreMatchers.containsString("denied_action3"));
            Assert.assertThat(e.getMessage(), not(CoreMatchers.containsString("accepted_action")));
        }
        List<SimulatePrincipalPolicyRequest> allSimulatePrincipalPolicyRequest = requestArgumentCaptor.getAllValues();
        int simulateRequestNumber = 6;
        assertEquals("expect if " + simulateRequestNumber + " simulate request has been sent",
                simulateRequestNumber, allSimulatePrincipalPolicyRequest.size());
        allSimulatePrincipalPolicyRequest.forEach(simulatePrincipalPolicyRequest ->
                assertEquals("arn", simulatePrincipalPolicyRequest.getPolicySourceArn()));

    }

    @Test
    public void verifyCredentialTest() throws IOException, AwsCredentialVerificationException {
        URL url = Resources.getResource("definitions/aws-cb-policy.json");
        String awsCbPolicy = Resources.toString(url, Charsets.UTF_8);
        when(awsPlatformParameters.getCredentialPoliciesJson()).thenReturn(Base64.getEncoder().encodeToString(awsCbPolicy.getBytes()));
        Map<String, Object> awsParameters = new HashMap<>();
        awsParameters.put("accessKey", "a");
        awsParameters.put("secretKey", "b");
        CloudCredential cloudCredential = new CloudCredential("id", "name", awsParameters);

        AmazonIdentityManagement amazonIdentityManagement = mock(AmazonIdentityManagement.class);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagement);

        AWSSecurityTokenService awsSecurityTokenService = mock(AWSSecurityTokenService.class);
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("arn");
        when(awsSecurityTokenService.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(getCallerIdentityResult);
        when(awsClient.createAwsSecurityTokenService(any(AwsCredentialView.class))).thenReturn(awsSecurityTokenService);

        ArgumentCaptor<SimulatePrincipalPolicyRequest> requestArgumentCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = new SimulatePrincipalPolicyResult();
            ArrayList<EvaluationResult> evaluationResults = new ArrayList<>();
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withEvalActionName("accepted_action1").withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withEvalActionName("accepted_action2").withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withEvalActionName("accepted_action3").withEvalResourceName("aws:ec2"));
            evaluationResults.add(new EvaluationResult().withEvalDecision("accept")
                    .withEvalActionName("accepted_action4").withEvalResourceName("*"));
            simulatePrincipalPolicyResult.setEvaluationResults(evaluationResults);
            when(amazonIdentityManagement.simulatePrincipalPolicy(requestArgumentCaptor.capture())).thenReturn(simulatePrincipalPolicyResult);

        awsCredentialVerifier.validateAws(new AwsCredentialView(cloudCredential));
    }
}
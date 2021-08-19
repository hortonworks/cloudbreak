package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.AutoScalingActions;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PolicyEvaluationDecisionType;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.ServiceFailureException;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.CloudIdentityType;

@ExtendWith(MockitoExtension.class)
public class AwsIamServiceTest {
    @Mock
    private AmazonIdentityManagementClient iam;

    @InjectMocks
    private AwsIamService awsIamService;

    @Test
    public void invalidInstanceProfileArn() {
        assertThat(awsIamService.getInstanceProfile(iam, null, null, null)).isNull();
        assertThat(awsIamService.getInstanceProfile(iam, "", null, null)).isNull();
        assertThat(awsIamService.getInstanceProfile(iam, "abc", null, null)).isNull();
    }

    @Test
    public void missingInstanceProfile() {
        when(iam.getInstanceProfile(any(GetInstanceProfileRequest.class))).thenThrow(NoSuchEntityException.class);

        String instanceProfileArn = "account/missingInstanceProfile";
        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn, CloudIdentityType.ID_BROKER,
                validationRequestBuilder);

        assertThat(instanceProfile).isNull();
        ValidationResult validationResult = validationRequestBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(
                Collections.singletonList(String.format("Instance profile (%s) doesn't exists on AWS side. " +
                                "Please check if you've used the correct Instance profile when setting up Data Access.",
                        instanceProfileArn)));
    }

    @Test
    public void instanceProfileServiceFailureException() {
        when(iam.getInstanceProfile(any(GetInstanceProfileRequest.class))).thenThrow(ServiceFailureException.class);

        String instanceProfileArn = "account/potentialInstanceProfile";
        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn, CloudIdentityType.ID_BROKER,
                validationRequestBuilder);

        assertThat(instanceProfile).isNull();
        ValidationResult validationResult = validationRequestBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(
                Collections.singletonList(String.format("Instance profile (%s) doesn't exists on AWS side. " +
                                "Please check if you've used the correct Instance profile when setting up Data Access.",
                        instanceProfileArn)));
    }

    @Test
    public void validInstanceProfile() {
        String instanceProfileArn = "account/validInstanceProfile";

        InstanceProfile expectedInstanceProfile = new InstanceProfile().withArn(instanceProfileArn);
        GetInstanceProfileResult getInstanceProfileResult = mock(GetInstanceProfileResult.class);
        when(getInstanceProfileResult.getInstanceProfile()).thenReturn(expectedInstanceProfile);
        when(iam.getInstanceProfile(any(GetInstanceProfileRequest.class))).thenReturn(getInstanceProfileResult);

        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn, CloudIdentityType.ID_BROKER,
                validationRequestBuilder);

        assertThat(instanceProfile.getArn()).isEqualTo(instanceProfileArn);
        assertThat(validationRequestBuilder.build().hasError()).isFalse();
    }

    @Test
    public void invalidRoleArn() {
        assertThat(awsIamService.getRole(iam, null, null)).isNull();
        assertThat(awsIamService.getRole(iam, "", null)).isNull();
        assertThat(awsIamService.getRole(iam, "abc", null)).isNull();
    }

    @Test
    public void missingRole() {
        when(iam.getRole(any(GetRoleRequest.class))).thenThrow(NoSuchEntityException.class);

        String roleArn = "account/missingRole";
        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        Role role = awsIamService.getRole(iam, roleArn, validationRequestBuilder);

        assertThat(role).isNull();
        ValidationResult validationResult = validationRequestBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(
                Collections.singletonList(String.format("Role (%s) doesn't exists on AWS side. " +
                        "Please check if you've used the correct Role when setting up Data Access.", roleArn)));
    }

    @Test
    public void roleServiceFailureException() {
        when(iam.getRole(any(GetRoleRequest.class))).thenThrow(ServiceFailureException.class);

        String roleArn = "account/potentialRole";
        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        Role role = awsIamService.getRole(iam, roleArn, validationRequestBuilder);

        assertThat(role).isNull();
        ValidationResult validationResult = validationRequestBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(
                Collections.singletonList(String.format("Role (%s) doesn't exists on AWS side. " +
                        "Please check if you've used the correct Role when setting up Data Access.", roleArn)));
    }

    @Test
    public void validRole() {
        String roleArn = "account/validRole";

        Role expectedRole = new Role().withArn(roleArn);
        GetRoleResult getRoleResult = mock(GetRoleResult.class);
        when(getRoleResult.getRole()).thenReturn(expectedRole);
        when(iam.getRole(any(GetRoleRequest.class))).thenReturn(getRoleResult);

        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        Role role = awsIamService.getRole(iam, roleArn, validationRequestBuilder);

        assertThat(role.getArn()).isEqualTo(roleArn);
        assertThat(validationRequestBuilder.build().hasError()).isFalse();
    }

    @Test
    public void testGetAssumeRolePolicyDocument() throws IOException {
        String assumeRolePolicyDocument = awsIamService.getResourceFileAsString(
                "json/aws-assume-role-policy-document.json");
        String encodedAssumeRolePolicyDocument = URLEncoder.encode(assumeRolePolicyDocument,
                StandardCharsets.UTF_8);


        Statement statement = new Statement(Effect.Allow).withId("1")
                .withPrincipals(new Principal("AWS", "arn:aws:iam::123456890:role/assume-role"))
                .withActions(SecurityTokenServiceActions.AssumeRole);
        Policy expectedAssumeRolePolicy = new Policy().withStatements(statement);

        Role role = mock(Role.class);
        when(role.getAssumeRolePolicyDocument()).thenReturn(encodedAssumeRolePolicyDocument);

        Policy assumeRolePolicy = awsIamService.getAssumeRolePolicy(role);
        assertThat(assumeRolePolicy).isNotNull();
        assertThat(assumeRolePolicy.toJson()).isEqualTo(expectedAssumeRolePolicy.toJson());
    }

    @Test
    public void testInvalidGetAssumeRolePolicyDocument() {
        assertThat(awsIamService.getAssumeRolePolicy(new Role())).isNull();
        Role role = new Role().withAssumeRolePolicyDocument("}garbage");
        assertThat(awsIamService.getAssumeRolePolicy(role)).isNull();
    }

    @Test
    public void testHandleTemplateReplacements() {
        assertThat(awsIamService.handleTemplateReplacements(null, Collections.emptyMap())).isNull();
        assertThat(awsIamService.handleTemplateReplacements("", Collections.emptyMap())).isEqualTo("");
        assertThat(awsIamService.handleTemplateReplacements("abc", Collections.emptyMap())).isEqualTo("abc");

        assertThat(awsIamService.handleTemplateReplacements("abc",
                Collections.singletonMap("abc", "def"))).isEqualTo("def");
        assertThat(awsIamService.handleTemplateReplacements("abcabc",
                Collections.singletonMap("abc", "def"))).isEqualTo("defdef");

        Map<String, String> replacements = Map.ofEntries(
                Map.entry("abc", "def"),
                Map.entry("ghi", "jkl")
        );
        assertThat(awsIamService.handleTemplateReplacements("abc ghi", replacements)).isEqualTo("def jkl");
    }

    @Test
    public void testGetPolicy() {
        assertThat(awsIamService.getPolicy("abc", Collections.emptyMap())).isNull();

        Policy expectedPolicyNoReplacements = new Policy().withStatements(
                new Statement(Effect.Allow).withId("FullObjectAccessUnderAuditDir")
                        .withActions(S3Actions.GetObject, S3Actions.PutObject)
                        .withResources(new Resource("arn:aws:s3:::${STORAGE_LOCATION_BASE}/ranger/audit/*")),
                new Statement(Effect.Allow).withId("LimitedAccessToDataLakeBucket")
                        .withActions(S3Actions.AbortMultipartUpload, S3Actions.ListObjects,
                                S3Actions.ListBucketMultipartUploads)
                        .withResources(new Resource("arn:aws:s3:::${DATALAKE_BUCKET}"))
        );
        assertThat(awsIamService.getPolicy("aws-cdp-ranger-audit-s3-policy.json",
                Collections.emptyMap()).toJson()).isEqualTo(expectedPolicyNoReplacements.toJson());

        Policy expectedPolicyWithReplacements = new Policy().withStatements(
                new Statement(Effect.Allow).withId("FullObjectAccessUnderAuditDir")
                        .withActions(S3Actions.GetObject, S3Actions.PutObject)
                        .withResources(new Resource("arn:aws:s3:::mybucket/mycluster/ranger/audit/*")),
                new Statement(Effect.Allow).withId("LimitedAccessToDataLakeBucket")
                        .withActions(S3Actions.AbortMultipartUpload, S3Actions.ListObjects,
                                S3Actions.ListBucketMultipartUploads)
                        .withResources(new Resource("arn:aws:s3:::mybucket"))
        );

        Map<String, String> policyReplacements = new HashMap<>();
        policyReplacements.put("${STORAGE_LOCATION_BASE}", "mybucket/mycluster");
        policyReplacements.put("${DATALAKE_BUCKET}", "mybucket");
        assertThat(awsIamService.getPolicy("aws-cdp-ranger-audit-s3-policy.json",
                policyReplacements).toJson()).isEqualTo(expectedPolicyWithReplacements.toJson());
    }

    @Test
    public void testGetStatementActions() {
        assertThat(awsIamService.getStatementActions(new Statement(Effect.Allow)))
                .isEqualTo(new TreeSet<>());

        SortedSet<String> expectedSingleAction = new TreeSet<>();
        expectedSingleAction.add(S3Actions.GetObject.getActionName());
        Statement statementSingleAction = new Statement(Effect.Allow).withActions(S3Actions.GetObject);
        assertThat(awsIamService.getStatementActions(statementSingleAction))
                .isEqualTo(expectedSingleAction);

        SortedSet<String> expectedMultipleActions = new TreeSet<>();
        expectedMultipleActions.add(S3Actions.GetObject.getActionName());
        expectedMultipleActions.add(S3Actions.PutObject.getActionName());
        Statement statementMultipleActions = new Statement(Effect.Allow)
                .withActions(S3Actions.GetObject, S3Actions.PutObject);
        assertThat(awsIamService.getStatementActions(statementMultipleActions))
                .isEqualTo(expectedMultipleActions);
    }

    @Test
    public void testGetStatementResources() {
        assertThat(awsIamService.getStatementResources(new Statement(Effect.Allow)))
                .isEqualTo(new TreeSet<>());

        SortedSet<String> expectedSingleResource = new TreeSet<>();
        expectedSingleResource.add("resource1");
        Statement statementSingleResouce = new Statement(Effect.Allow)
                .withResources(new Resource("resource1"));
        assertThat(awsIamService.getStatementResources(statementSingleResouce))
                .isEqualTo(expectedSingleResource);

        SortedSet<String> expectedMultipleResources = new TreeSet<>();
        expectedMultipleResources.add("resource1");
        expectedMultipleResources.add("resource2");
        Statement statementMultipleResources = new Statement(Effect.Allow)
                .withResources(
                        new Resource("resource1"),
                        new Resource("resource2"));
        assertThat(awsIamService.getStatementResources(statementMultipleResources))
                .isEqualTo(expectedMultipleResources);
    }

    @Test
    public void testValidateRolePolicies() {
        ArgumentCaptor<SimulatePrincipalPolicyRequest> simulatePolicyRequestCaptor = ArgumentCaptor.forClass(SimulatePrincipalPolicyRequest.class);
        when(iam.simulatePrincipalPolicy(simulatePolicyRequestCaptor.capture()))
                .thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(new EvaluationResult()
                        .withEvalDecision(PolicyEvaluationDecisionType.Allowed)));
        Policy policy1 = new Policy();
        Set<Statement> statements1 = Set.of(
                createStatement(Set.of(AutoScalingActions.CreateAutoScalingGroup, AutoScalingActions.DeleteAutoScalingGroup),
                        Set.of(new Resource("resource1"), new Resource("resource2"))),
                createStatement(Set.of(S3Actions.GetObject, S3Actions.PutObject),
                        Set.of(new Resource("resource1"), new Resource("resource2"), new Resource("resource3"), new Resource("resource4")))
        );
        policy1.getStatements().addAll(statements1);
        Policy policy2 = new Policy();
        Set<Statement> statements2 = Set.of(
                createStatement(Set.of(AutoScalingActions.AttachLoadBalancers, AutoScalingActions.AttachLoadBalancerTargetGroups),
                        Set.of(new Resource("resource1"), new Resource("resource2"))),
                createStatement(Set.of(S3Actions.GetObject, S3Actions.PutObject),
                        Set.of(new Resource("resource5"))),
                createStatement(Set.of(S3Actions.GetObject, S3Actions.PutObject),
                        Set.of(new Resource("resource6")))
        );
        policy2.setStatements(statements2);
        Set<Policy> policies = Set.of(policy1, policy2);

        List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam, createRole(), policies);

        assertEquals(2, evaluationResults.size());
        verify(iam, times(2)).simulatePrincipalPolicy(any());
        List<SimulatePrincipalPolicyRequest> requests = simulatePolicyRequestCaptor.getAllValues();

        SimulatePrincipalPolicyRequest request1;
        SimulatePrincipalPolicyRequest request2;
        if (requests.get(0).getResourceArns().size() == 6) {
            request1 = requests.get(0);
            request2 = requests.get(1);
        } else {
            request1 = requests.get(1);
            request2 = requests.get(0);
        }
        assertEquals("roleArn", request1.getPolicySourceArn());
        assertEquals("roleArn", request2.getPolicySourceArn());
        assertThat(request1.getResourceArns()).containsExactlyInAnyOrder("resource1", "resource2", "resource3", "resource4", "resource5", "resource6");
        assertThat(request2.getResourceArns()).containsExactlyInAnyOrder("resource1", "resource2");
        assertThat(request1.getActionNames()).containsExactlyInAnyOrder("s3:GetObject", "s3:PutObject");
        assertThat(request2.getActionNames()).containsExactlyInAnyOrder("autoscaling:CreateAutoScalingGroup", "autoscaling:DeleteAutoScalingGroup",
                "autoscaling:AttachLoadBalancers", "autoscaling:AttachLoadBalancerTargetGroups");
    }

    @Test
    public void testInvalidValidateRolePolicies() {
        when(iam.simulatePrincipalPolicy(any()))
                .thenReturn(new SimulatePrincipalPolicyResult().withEvaluationResults(new EvaluationResult()
                        .withEvalDecision(PolicyEvaluationDecisionType.ExplicitDeny)));

        Policy policy = new Policy().withStatements(createStatement(Set.of(S3Actions.GetObject), Set.of(new Resource("resource1"))));
        List<EvaluationResult> evaluationResults = awsIamService.validateRolePolicies(iam, createRole(), Set.of(policy));
        assertThat(evaluationResults).hasSize(1);
        EvaluationResult evaluationResult = evaluationResults.get(0);
        assertEquals("explicitDeny", evaluationResult.getEvalDecision());
        verify(iam, times(1)).simulatePrincipalPolicy(any());
    }

    private Statement createStatement(Set<Action> actions, Set<Resource> resources) {
        Statement statement = new Statement(Effect.Allow);
        statement.getActions().addAll(actions);
        statement.setResources(resources);
        return statement;
    }

    private Role createRole() {
        return new Role().withArn("roleArn");
    }
}

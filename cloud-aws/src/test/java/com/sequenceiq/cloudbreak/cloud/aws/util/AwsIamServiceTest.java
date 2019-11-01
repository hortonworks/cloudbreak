package com.sequenceiq.cloudbreak.cloud.aws.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.ServiceFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@ExtendWith(MockitoExtension.class)
public class AwsIamServiceTest {
    @Mock
    private AmazonIdentityManagement iam;

    @InjectMocks
    private AwsIamService awsIamService;

    @Test
    public void invalidInstanceProfileArn() {
        assertThat(awsIamService.getInstanceProfile(iam, null, null)).isNull();
        assertThat(awsIamService.getInstanceProfile(iam, "", null)).isNull();
        assertThat(awsIamService.getInstanceProfile(iam, "abc", null)).isNull();
    }

    @Test
    public void missingInstanceProfile() {
        when(iam.getInstanceProfile(any(GetInstanceProfileRequest.class))).thenThrow(NoSuchEntityException.class);

        String instanceProfileArn = "account/missingInstanceProfile";
        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn,
            validationRequestBuilder);

        assertThat(instanceProfile).isNull();
        ValidationResult validationResult = validationRequestBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(
            Collections.singletonList(String.format("Instance profile (%s) doesn't exist.",
                instanceProfileArn)));
    }

    @Test
    public void instanceProfileServiceFailureException() {
        when(iam.getInstanceProfile(any(GetInstanceProfileRequest.class))).thenThrow(ServiceFailureException.class);

        String instanceProfileArn = "account/potentialInstanceProfile";
        ValidationResultBuilder validationRequestBuilder = new ValidationResultBuilder();
        InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn,
            validationRequestBuilder);

        assertThat(instanceProfile).isNull();
        ValidationResult validationResult = validationRequestBuilder.build();
        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getErrors()).isEqualTo(
            Collections.singletonList(String.format("Instance profile (%s) doesn't exist.",
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
        InstanceProfile instanceProfile = awsIamService.getInstanceProfile(iam, instanceProfileArn,
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
            Collections.singletonList(String.format("Role (%s) doesn't exist.", roleArn)));
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
            Collections.singletonList(String.format("Role (%s) doesn't exist.", roleArn)));
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
}

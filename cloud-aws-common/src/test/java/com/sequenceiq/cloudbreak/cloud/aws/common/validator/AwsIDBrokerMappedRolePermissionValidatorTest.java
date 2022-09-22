package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.OrganizationsDecisionDetail;
import software.amazon.awssdk.services.iam.model.PolicyEvaluationDecisionType;
import software.amazon.awssdk.services.iam.model.Role;

public abstract class AwsIDBrokerMappedRolePermissionValidatorTest {

    public abstract AwsIDBrokerMappedRolePermissionValidator getValidator();

    public abstract void testGetUsers();

    public abstract void testGetPolicyFileNames();

    public abstract void testGetStorageLocationBase();

    public abstract void testCheckLocation();

    public abstract void testGetPolicyJsonReplacements();

    public abstract void testGetBackupPolicyJsonReplacements();

    public abstract void testGetBackupPolicyJsonReplacementsWithEmptyBackupLocation();

    public abstract void testCollectBackupPolicies();

    public abstract void testGetPolicyJsonReplacementsNoDynamodb();

    public abstract void testCollectPolicies();

    @Test
    public void testGetRoleArnsForUsers() {
        assertThat(getValidator().getRoleArnsForUsers(Collections.emptyList(),
                Collections.emptyMap())).isEqualTo(Collections.emptySortedSet());

        List<String> users = Arrays.asList("user1", "user2", "user3");
        assertThat(getValidator().getRoleArnsForUsers(users,
                Collections.emptyMap())).isEqualTo(Collections.emptySortedSet());

        Map<String, String> userMappings = new TreeMap<>();
        userMappings.put("user1", "role1");
        userMappings.put("user2", "role1");
        userMappings.put("user3", "role2");
        List<String> allUsers = new ArrayList<>(userMappings.keySet());
        SortedSet<String> expectedRoles = new TreeSet<>(userMappings.values());
        assertThat(getValidator().getRoleArnsForUsers(allUsers, userMappings))
                .isEqualTo(expectedRoles);

        List<String> oneUser = Collections.singletonList("user1");
        SortedSet<String> expectedOneRole = new TreeSet<>();
        expectedOneRole.add("role1");
        assertThat(getValidator().getRoleArnsForUsers(oneUser, userMappings))
                .isEqualTo(expectedOneRole);
    }

    @Test
    public void testPolicies() {
        assertThat(getValidator().getPolicies(Collections.emptyList(),
                Collections.emptyMap())).isEqualTo(Collections.emptyList());
    }

    @Test
    public void testGetFailedActionsWhenSkipOrgPolicyDecisionsIsFalse() {
        Role role = Role.builder().arn("testRole").build();
        EvaluationResult allowEvalResult = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("goodResource")
                .evalDecision(PolicyEvaluationDecisionType.ALLOWED).build();
        EvaluationResult denyEvalResult = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY).build();
        EvaluationResult denyOrganizationsDecisionEvalResult = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY).build();

        assertThat(getValidator().getFailedActions(role, Collections.emptyList(), false)).isEqualTo(Collections.emptySortedSet());

        List<EvaluationResult> allowEvalResults = Collections.singletonList(allowEvalResult);
        assertThat(getValidator().getFailedActions(role, allowEvalResults, false)).isEqualTo(Collections.emptySortedSet());

        SortedSet<String> expectedFailedActions = new TreeSet<>();
        expectedFailedActions.add(String.format("%s:%s:%s", role.arn(),
                denyEvalResult.evalActionName(), denyEvalResult.evalResourceName()));
        expectedFailedActions.add(String.format("%s:%s:%s", role.arn(),
                denyEvalResult.evalActionName(), denyEvalResult.evalResourceName() + " -> Denied by Organization Rule"));
        List<EvaluationResult> denyEvalResults = List.of(denyEvalResult, denyOrganizationsDecisionEvalResult);
        assertThat(getValidator().getFailedActions(role, denyEvalResults, false)) .isEqualTo(expectedFailedActions);

        List<EvaluationResult> multipleEvalResults = Arrays.asList(denyEvalResult,
                allowEvalResult, denyEvalResult, denyEvalResult, denyOrganizationsDecisionEvalResult, allowEvalResult);
        assertThat(getValidator().getFailedActions(role, multipleEvalResults, false)).isEqualTo(expectedFailedActions);
    }

    @Test
    public void testGetFailedActionsWhenSkipOrgPolicyDecisionsIsTrue() {
        Role role = Role.builder().arn("testRole").build();
        EvaluationResult denyEvalResult = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .build();
        EvaluationResult denyOrganizationsDecisionEvalResult = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .build();

        List<EvaluationResult> denyOrganizationResultOnly = List.of(denyOrganizationsDecisionEvalResult);
        assertThat(getValidator().getFailedActions(role, denyOrganizationResultOnly, true)) .isEmpty();

        SortedSet<String> expectedFailedActions = new TreeSet<>();
        expectedFailedActions.add(String.format("%s:%s:%s", role.arn(),
                denyEvalResult.evalActionName(), denyEvalResult.evalResourceName()));
        List<EvaluationResult> denyEvalResults = List.of(denyEvalResult, denyOrganizationsDecisionEvalResult);
        assertThat(getValidator().getFailedActions(role, denyEvalResults, true)) .isEqualTo(expectedFailedActions);
    }

    @Test
    public void testShouldSkipOrgPolicyDeny() {
        EvaluationResult denyEvalResultWithoutOrgDetails = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .build();
        EvaluationResult denyEvalResultWithOrgDetailsAllow = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .build();
        EvaluationResult denyEvalResultWithOrgDetailsDeny = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                .build();
        assertFalse(getValidator().shouldSkipOrgPolicyDeny(denyEvalResultWithoutOrgDetails, false));
        assertFalse(getValidator().shouldSkipOrgPolicyDeny(denyEvalResultWithOrgDetailsAllow, false));
        assertFalse(getValidator().shouldSkipOrgPolicyDeny(denyEvalResultWithOrgDetailsDeny, false));
        assertFalse(getValidator().shouldSkipOrgPolicyDeny(denyEvalResultWithoutOrgDetails, true));
        assertFalse(getValidator().shouldSkipOrgPolicyDeny(denyEvalResultWithOrgDetailsAllow, true));
        assertTrue(getValidator().shouldSkipOrgPolicyDeny(denyEvalResultWithOrgDetailsDeny, true));
    }

    @Test
    public void testShouldCheckValidationResult() {
        EvaluationResult denyEvalResultWithoutOrgDetails = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .build();
        EvaluationResult denyEvalResultWithOrgDetailsAllow = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(true).build())
                .build();
        EvaluationResult denyEvalResultWithOrgDetailsDeny = EvaluationResult.builder()
                .evalActionName("doAction")
                .evalResourceName("badResource")
                .evalDecision(PolicyEvaluationDecisionType.IMPLICIT_DENY)
                .organizationsDecisionDetail(OrganizationsDecisionDetail.builder().allowedByOrganizations(false).build())
                .build();
        assertTrue(getValidator().shouldCheckEvaluationResult(denyEvalResultWithoutOrgDetails, false));
        assertTrue(getValidator().shouldCheckEvaluationResult(denyEvalResultWithOrgDetailsAllow, false));
        assertTrue(getValidator().shouldCheckEvaluationResult(denyEvalResultWithOrgDetailsDeny, false));
        assertTrue(getValidator().shouldCheckEvaluationResult(denyEvalResultWithoutOrgDetails, true));
        assertTrue(getValidator().shouldCheckEvaluationResult(denyEvalResultWithOrgDetailsAllow, true));
        assertFalse(getValidator().shouldCheckEvaluationResult(denyEvalResultWithOrgDetailsDeny, true));
    }
}

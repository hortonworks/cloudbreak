package com.sequenceiq.cloudbreak.cloud.aws.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.PolicyEvaluationDecisionType;
import com.amazonaws.services.identitymanagement.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class AwsIDBrokerMappedRolePermissionValidatorTest {
    @Mock
    protected AmazonIdentityManagement iam;

    public abstract AwsIDBrokerMappedRolePermissionValidator getValidator();

    @Test
    public abstract void testGetUsers();

    @Test
    public abstract void testGetPolicyFileNames();

    @Test
    public abstract void testGetStorageLocationBase();

    @Test
    public abstract void testCheckLocation();

    @Test
    public abstract void testGetPolicyJsonReplacements();

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
    public void testGetFailedActions() {
        Role role = new Role().withArn("testRole");
        EvaluationResult allowEvalResult = new EvaluationResult()
                                                .withEvalActionName("doAction")
                                                .withEvalResourceName("goodResource")
                                                .withEvalDecision(PolicyEvaluationDecisionType.Allowed);
        EvaluationResult denyEvalResult = new EvaluationResult()
                                                .withEvalActionName("doAction")
                                                .withEvalResourceName("badResource")
                                                .withEvalDecision(PolicyEvaluationDecisionType.ImplicitDeny);

        assertThat(getValidator().getFailedActions(role,
            Collections.emptyList())).isEqualTo(Collections.emptySortedSet());

        List<EvaluationResult> allowEvalResults = Collections.singletonList(allowEvalResult);
        assertThat(getValidator().getFailedActions(role,
            allowEvalResults)).isEqualTo(Collections.emptySortedSet());

        SortedSet<String> expectedFailedActions = new TreeSet<>();
        expectedFailedActions.add(String.format("%s:%s:%s", role.getArn(),
            denyEvalResult.getEvalActionName(), denyEvalResult.getEvalResourceName()));
        List<EvaluationResult> denyEvalResults = Collections.singletonList(denyEvalResult);
        assertThat(getValidator().getFailedActions(role, denyEvalResults))
            .isEqualTo(expectedFailedActions);

        List<EvaluationResult> multipleEvalResults = Arrays.asList(denyEvalResult,
            allowEvalResult, denyEvalResult, denyEvalResult, allowEvalResult);
        assertThat(getValidator().getFailedActions(role, multipleEvalResults))
            .isEqualTo(expectedFailedActions);
    }
}

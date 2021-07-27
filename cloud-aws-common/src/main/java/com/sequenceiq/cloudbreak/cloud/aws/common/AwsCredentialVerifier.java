package com.sequenceiq.cloudbreak.cloud.aws.common;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.internal.JsonPolicyReader;
import com.amazonaws.services.identitymanagement.model.ContextEntry;
import com.amazonaws.services.identitymanagement.model.ContextKeyTypeEnum;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsCredentialCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

@Service
public class AwsCredentialVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialVerifier.class);

    private static final int MAX_ELEMENT_SIZE = 200;

    @Inject
    private CommonAwsClient awsClient;

    @Cacheable(value = AwsCredentialCachingConfig.TEMPORARY_AWS_CREDENTIAL_VERIFIER_CACHE,
            unless = "#awsCredential == null")
    public void validateAws(AwsCredentialView awsCredential, String policyJson) throws AwsPermissionMissingException {
        String policies = new String(Base64.getDecoder().decode(policyJson));
        try {
            List<RequiredAction> resourcesWithActions = getRequiredActions(policies);
            AmazonIdentityManagementClient amazonIdentityManagement = awsClient.createAmazonIdentityManagement(awsCredential);
            AmazonSecurityTokenServiceClient awsSecurityTokenService = awsClient.createSecurityTokenService(awsCredential);
            String arn;
            if (awsCredential.getRoleArn() != null) {
                arn = awsCredential.getRoleArn();
            } else {
                GetCallerIdentityResult callerIdentity = awsSecurityTokenService.getCallerIdentity(new GetCallerIdentityRequest());
                arn = callerIdentity.getArn();
            }

            List<String> failedActionList = new ArrayList<>();
            for (RequiredAction resourceAndAction : resourcesWithActions) {
                SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest = new SimulatePrincipalPolicyRequest();
                simulatePrincipalPolicyRequest.setMaxItems(MAX_ELEMENT_SIZE);
                simulatePrincipalPolicyRequest.setPolicySourceArn(arn);
                simulatePrincipalPolicyRequest.setActionNames(resourceAndAction.getActionNames());
                simulatePrincipalPolicyRequest.setResourceArns(Collections.singleton(resourceAndAction.getResourceArn()));
                simulatePrincipalPolicyRequest.setContextEntries(resourceAndAction.getConditions());
                LOGGER.debug("Simulate policy request: {}", simulatePrincipalPolicyRequest);
                SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = amazonIdentityManagement.simulatePrincipalPolicy(simulatePrincipalPolicyRequest);
                LOGGER.debug("Simulate policy result: {}", simulatePrincipalPolicyResult);
                simulatePrincipalPolicyResult.getEvaluationResults().stream()
                        .filter(evaluationResult -> evaluationResult.getEvalDecision().toLowerCase().contains("deny"))
                        .map(evaluationResult -> {
                            if (evaluationResult.getOrganizationsDecisionDetail() != null && !evaluationResult.getOrganizationsDecisionDetail()
                                    .getAllowedByOrganizations()) {
                                return evaluationResult.getEvalActionName() + " : " + evaluationResult.getEvalResourceName() + " -> Denied by Organization Rule";
                            } else {
                                return evaluationResult.getEvalActionName() + " : " + evaluationResult.getEvalResourceName();
                            }
                        })
                        .forEach(failedActionList::add);
            }
            if (!failedActionList.isEmpty()) {
                throw new AwsPermissionMissingException(String.format("CDP Credential '%s' doesn't have permission for these actions which are required: %s",
                        awsCredential.getName(), failedActionList.stream().collect(joining(", ", "[ ", " ]"))));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can not parse aws policy json", e);
        }
    }

    private List<RequiredAction> getRequiredActions(String policies) throws IOException {
        List<RequiredAction> requiredActions = new ArrayList<>();
        Policy policy = new JsonPolicyReader().createPolicyFromJsonString(policies);
        for (Statement statement : policy.getStatements()) {
            RequiredAction requiredAction = new RequiredAction();
            List<Action> actions = statement.getActions();
            if (actions != null) {
                List<String> actionNames = actions.stream()
                        .map(e -> e.getActionName())
                        .collect(Collectors.toList());
                requiredAction.setActionNames(actionNames);
            }
            List<Condition> conditions = statement.getConditions();
            if (conditions != null) {
                for (Condition condition : conditions) {
                    ContextEntry contextEntry = new ContextEntry();
                    contextEntry.setContextKeyName(condition.getConditionKey());
                    contextEntry.setContextKeyType(ContextKeyTypeEnum.String);
                    contextEntry.setContextKeyValues(condition.getValues());
                    requiredAction.getConditions().add(contextEntry);
                }
            }
            String resourceString = statement.getResources().stream()
                    .findFirst()
                    .get()
                    .getId();
            requiredAction.setResourceArn(resourceString);

            Optional<RequiredAction> first = requiredActions.stream()
                    .filter(e -> e.getConditions().equals(requiredAction.getConditions())
                            && e.getResourceArn().equals(requiredAction.getResourceArn()))
                    .findFirst();

            if (first.isPresent()) {
                requiredActions.remove(first.get());
                requiredAction.getActionNames().addAll(first.get().getActionNames());
                requiredAction.getConditions().addAll(first.get().getConditions());
                requiredActions.add(requiredAction);
            } else {
                requiredActions.add(requiredAction);
            }
        }
        return requiredActions;
    }
}

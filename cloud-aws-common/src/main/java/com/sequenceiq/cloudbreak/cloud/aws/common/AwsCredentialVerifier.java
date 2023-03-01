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

import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsCredentialCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.core.auth.policy.Action;
import software.amazon.awssdk.core.auth.policy.Condition;
import software.amazon.awssdk.core.auth.policy.Policy;
import software.amazon.awssdk.core.auth.policy.Statement;
import software.amazon.awssdk.core.auth.policy.internal.JsonPolicyReader;
import software.amazon.awssdk.services.iam.model.ContextEntry;
import software.amazon.awssdk.services.iam.model.ContextKeyTypeEnum;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

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
            String arn = getString(awsCredential, awsSecurityTokenService);

            List<String> failedActionList = new ArrayList<>();
            for (RequiredAction resourceAndAction : resourcesWithActions) {
                SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest = SimulatePrincipalPolicyRequest.builder()
                        .maxItems(MAX_ELEMENT_SIZE)
                        .policySourceArn(arn)
                        .actionNames(resourceAndAction.getActionNames())
                        .resourceArns(Collections.singleton(resourceAndAction.getResourceArn()))
                        .contextEntries(resourceAndAction.getConditions())
                        .build();
                LOGGER.debug("Simulate policy request: {}", simulatePrincipalPolicyRequest);
                SimulatePrincipalPolicyResponse simulatePrincipalPolicyResponse = amazonIdentityManagement.simulatePrincipalPolicy(
                        simulatePrincipalPolicyRequest);
                LOGGER.debug("Simulate policy result: {}", simulatePrincipalPolicyResponse);
                simulatePrincipalPolicyResponse.evaluationResults().stream()
                        .filter(evaluationResponse -> evaluationResponse.evalDecisionAsString().toLowerCase().contains("deny"))
                        .map(evaluationResponse -> {
                            if (evaluationResponse.organizationsDecisionDetail() != null && !evaluationResponse.organizationsDecisionDetail()
                                    .allowedByOrganizations()) {
                                return evaluationResponse.evalActionName() + " : " + evaluationResponse.evalResourceName() + " -> Denied by Organization Rule";
                            } else {
                                return evaluationResponse.evalActionName() + " : " + evaluationResponse.evalResourceName();
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

    private String getString(AwsCredentialView awsCredential, AmazonSecurityTokenServiceClient awsSecurityTokenService) {
        return awsCredential.getRoleArn() != null ?
                awsCredential.getRoleArn() :
                awsSecurityTokenService.getCallerIdentity(GetCallerIdentityRequest.builder().build()).arn();
    }

    private List<RequiredAction> getRequiredActions(String policies) throws IOException {
        List<RequiredAction> requiredActions = new ArrayList<>();
        Policy policy = new JsonPolicyReader().createPolicyFromJsonString(policies);
        for (Statement statement : policy.getStatements()) {
            RequiredAction requiredAction = new RequiredAction();
            List<Action> actions = statement.getActions();
            if (actions != null) {
                List<String> actionNames = actions.stream()
                        .map(Action::getActionName)
                        .collect(Collectors.toList());
                requiredAction.setActionNames(actionNames);
            }
            List<Condition> conditions = statement.getConditions();
            if (conditions != null) {
                for (Condition condition : conditions) {
                    ContextEntry contextEntry = ContextEntry.builder()
                            .contextKeyName(condition.getConditionKey())
                            .contextKeyType(ContextKeyTypeEnum.STRING)
                            .contextKeyValues(condition.getValues())
                            .build();
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

package com.sequenceiq.cloudbreak.cloud.aws;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.aws.cache.AwsCredentialCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class AwsCredentialVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialVerifier.class);

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsClient awsClient;

    @Cacheable(value = AwsCredentialCachingConfig.TEMPORARY_AWS_CREDENTIAL_VERIFIER_CACHE,
            unless = "#awsCredential == null")
    public void validateAws(AwsCredentialView awsCredential) throws AwsPermissionMissingException {
        String policies = new String(Base64.getDecoder().decode(awsPlatformParameters.getCredentialPoliciesJson()));
        try {
            Map<String, List<String>> resourcesWithActions = getRequiredActions(policies);
            AmazonIdentityManagement amazonIdentityManagement = awsClient.createAmazonIdentityManagement(awsCredential);
            AWSSecurityTokenService awsSecurityTokenService = awsClient.createAwsSecurityTokenService(awsCredential);
            String arn;
            if (awsCredential.getRoleArn() != null) {
                arn = awsCredential.getRoleArn();
            } else {
                GetCallerIdentityResult callerIdentity = awsSecurityTokenService.getCallerIdentity(new GetCallerIdentityRequest());
                arn = callerIdentity.getArn();
            }

            List<String> failedActionList = new ArrayList<>();
            for (Map.Entry<String, List<String>> resourceAndAction : resourcesWithActions.entrySet()) {
                SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest = new SimulatePrincipalPolicyRequest();
                simulatePrincipalPolicyRequest.setPolicySourceArn(arn);
                simulatePrincipalPolicyRequest.setActionNames(resourceAndAction.getValue());
                simulatePrincipalPolicyRequest.setResourceArns(Collections.singleton(resourceAndAction.getKey()));
                LOGGER.debug("Simulate policy request: {}", simulatePrincipalPolicyRequest);
                SimulatePrincipalPolicyResult simulatePrincipalPolicyResult = amazonIdentityManagement.simulatePrincipalPolicy(simulatePrincipalPolicyRequest);
                LOGGER.debug("Simulate policy result: {}", simulatePrincipalPolicyResult);
                simulatePrincipalPolicyResult.getEvaluationResults().stream()
                        .filter(evaluationResult -> evaluationResult.getEvalDecision().toLowerCase().contains("deny"))
                        .map(evaluationResult -> evaluationResult.getEvalActionName() + ":" + evaluationResult.getEvalResourceName())
                        .forEach(failedActionList::add);
            }
            if (!failedActionList.isEmpty()) {
                throw new AwsPermissionMissingException(String.format("CDP Credential '%s' doesn't have permission for these actions which are required: %s",
                        awsCredential.getName(), failedActionList));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can not parse aws policy json", e);
        }
    }

    private Map<String, List<String>> getRequiredActions(String policies) throws IOException {
        JsonNode jsonNode = JsonUtil.readTree(policies);
        JsonNode statements = jsonNode.get("Statement");
        return StreamSupport.stream(statements.spliterator(), false)
                .map(statement -> new AbstractMap.SimpleEntry<>(statement.get("Resource"),
                        StreamSupport.stream(statement.get("Action").spliterator(), false)
                                .map(JsonNode::asText)
                                .collect(Collectors.toList())))
                .flatMap(entry -> StreamSupport.stream(entry.getKey().spliterator(), false)
                        .map(node -> new AbstractMap.SimpleEntry<>(node.asText(), entry.getValue())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (value1, value2) -> {
                    value1.addAll(value2);
                    return value1;
                }));
    }
}

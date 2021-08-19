package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsValidationMessageUtil.getAdviceMessage;
import static com.sequenceiq.common.model.CloudIdentityType.ID_BROKER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.GetInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.ServiceFailureException;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAccessConfigType;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.model.CloudIdentityType;

@Service
public class AwsIamService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIamService.class);

    private static final String POLICY_BASE_LOCATION = "definitions/cdp/";

    /**
     * Validates instance profile ARN and returns an InstanceProfile object if valid
     *
     * @param iam                     AmazonIdentityManagement client
     * @param instanceProfileArn      instance profile ARN
     * @param validationResultBuilder builder for any errors encountered
     * @return InstanceProfile if instance profile ARN is valid otherwise null
     */
    public InstanceProfile getInstanceProfile(AmazonIdentityManagementClient iam, String instanceProfileArn,
            CloudIdentityType cloudIdentityType, ValidationResultBuilder validationResultBuilder) {
        InstanceProfile instanceProfile = null;
        if (instanceProfileArn != null && instanceProfileArn.contains("/")) {
            String instanceProfileName = instanceProfileArn.split("/", 2)[1];
            GetInstanceProfileRequest instanceProfileRequest = new GetInstanceProfileRequest()
                    .withInstanceProfileName(instanceProfileName);
            try {
                instanceProfile = iam.getInstanceProfile(instanceProfileRequest).getInstanceProfile();
            } catch (NoSuchEntityException | ServiceFailureException e) {
                String msg = String.format("Instance profile (%s) doesn't exists on AWS side. " +
                        getAdviceMessage(AwsAccessConfigType.INSTANCE_PROFILE, cloudIdentityType), instanceProfileArn);
                LOGGER.error(msg, e);
                validationResultBuilder.error(msg);
            }
        }
        return instanceProfile;
    }

    /**
     * Returns valid roles from a set of role ARNs
     *
     * @param iam                     AmazonIdentityManagement client
     * @param roleArns                set of role ARNs
     * @param validationResultBuilder builder for any errors encountered
     * @return set of valid Role objects
     */
    public Set<Role> getValidRoles(AmazonIdentityManagementClient iam, Set<String> roleArns,
            ValidationResultBuilder validationResultBuilder) {
        return roleArns.stream()
                .map(roleArn -> getRole(iam, roleArn, validationResultBuilder))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Validates role ARN and returns an Role object if valid
     *
     * @param iam                     AmazonIdentityManagement client
     * @param roleArn                 role ARN
     * @param validationResultBuilder builder for any errors encountered
     * @return Role if role ARN is valid otherwise null
     */
    public Role getRole(AmazonIdentityManagementClient iam, String roleArn,
            ValidationResultBuilder validationResultBuilder) {
        Role role = null;
        if (roleArn != null && roleArn.contains("/")) {
            String roleName = roleArn.split("/", 2)[1];
            GetRoleRequest roleRequest = new GetRoleRequest().withRoleName(roleName);
            try {
                role = iam.getRole(roleRequest).getRole();
            } catch (NoSuchEntityException | ServiceFailureException e) {
                String msg = String.format("Role (%s) doesn't exists on AWS side. " + getAdviceMessage(AwsAccessConfigType.ROLE, ID_BROKER), roleArn);
                LOGGER.debug(msg, e);
                validationResultBuilder.error(msg);
            }
        }
        return role;
    }

    /**
     * Gets the role assume role policy document as a Policy object
     *
     * @param role Role to evaluate
     * @return assume role Policy object
     */
    public Policy getAssumeRolePolicy(Role role) {
        Policy policy = null;
        String assumeRolePolicyDocument = role.getAssumeRolePolicyDocument();
        if (assumeRolePolicyDocument != null) {
            try {
                String decodedAssumeRolePolicyDocument = URLDecoder.decode(assumeRolePolicyDocument,
                        StandardCharsets.UTF_8);
                policy = Policy.fromJson(decodedAssumeRolePolicyDocument);
            } catch (IllegalArgumentException e) {
                LOGGER.error(String.format("Unable to get policy from role (%s)", role.getArn()), e);
            }
        }

        return policy;
    }

    /**
     * Replace template with strings from replacements map
     *
     * @param template     string of the template
     * @param replacements map of simple replacements to make to template
     * @return template with replacements replaced
     */
    String handleTemplateReplacements(String template, Map<String, String> replacements) {
        String replacedTemplate = template;
        if (replacedTemplate != null) {
            for (Entry<String, String> replacement : replacements.entrySet()) {
                String replacementValue = replacement.getValue() != null ? replacement.getValue() : "";
                replacedTemplate = replacedTemplate.replace(replacement.getKey(), replacementValue);
            }
        }
        return replacedTemplate;
    }

    /**
     * Returns a Policy object that has replacements made to the template json
     *
     * @param policyFileName Policy template file name
     * @param replacements   map of simple replacements to make to policy template json
     * @return Policy with replacements made
     */
    public Policy getPolicy(String policyFileName, Map<String, String> replacements) {
        Policy policy = null;
        try {
            String policyJsonTemplate = getResourceFileAsString(POLICY_BASE_LOCATION + policyFileName);
            String policyJson = handleTemplateReplacements(policyJsonTemplate, replacements);
            if (policyJson != null) {
                policy = Policy.fromJson(policyJson);
            }
        } catch (IOException e) {
            LOGGER.debug("Unable to load policy json", e);
        }

        return policy;
    }

    /**
     * Returns actions from the given statement
     *
     * @param statement statement to get actions from
     * @return sorted set of actions
     */
    public SortedSet<String> getStatementActions(Statement statement) {
        List<Action> actions = statement.getActions();
        if (actions == null) {
            return new TreeSet<>();
        }
        return statement.getActions().stream()
                .map(Action::getActionName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns resources from the given statement
     *
     * @param statement statement to get resources from
     * @return sorted set of resources
     */
    public SortedSet<String> getStatementResources(Statement statement) {
        List<Resource> resources = statement.getResources();
        if (resources == null) {
            return new TreeSet<>();
        }
        return resources.stream()
                .map(Resource::getId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Helper method that wraps simulating a principal policy
     *
     * @param iam             AmazonIdentityManagement client
     * @param policySourceArn arn to to check against
     * @param actionNames     actions to simulate
     * @param resourceArns    resources to simulate
     * @return List of evaluation results
     */
    public List<EvaluationResult> simulatePrincipalPolicy(AmazonIdentityManagementClient iam,
            String policySourceArn, Collection<String> actionNames, Collection<String> resourceArns)
            throws AmazonIdentityManagementException {
        SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest =
                new SimulatePrincipalPolicyRequest()
                        .withPolicySourceArn(policySourceArn)
                        .withActionNames(actionNames)
                        .withResourceArns(resourceArns);
        SimulatePrincipalPolicyResult simulatePrincipalPolicyResult =
                iam.simulatePrincipalPolicy(simulatePrincipalPolicyRequest);
        return simulatePrincipalPolicyResult.getEvaluationResults();
    }

    /**
     * Validates the given roles against the policies
     *
     * @param iam      AmazonIdentityManagement client
     * @param role     Role object to check
     * @param policies collection of Policy objects to check
     * @return list of evaluation results
     */
    public List<EvaluationResult> validateRolePolicies(AmazonIdentityManagementClient iam, Role role,
            Collection<Policy> policies) throws AmazonIdentityManagementException {
        List<EvaluationResult> evaluationResults = new ArrayList<>();
        Set<PolicySimulation> policySimulations = collectPolicySimulations(policies);
        for (PolicySimulation policySimulation : policySimulations) {
            List<EvaluationResult> results = simulatePrincipalPolicy(iam, role.getArn(), policySimulation.getActions(), policySimulation.getResources());
            evaluationResults.addAll(results);
        }
        return evaluationResults;
    }

    private Set<PolicySimulation> collectPolicySimulations(Collection<Policy> policies) {
        Map<Set<String>, PolicySimulation> policySimulationsByResources = mergePolicySimulationsByResources(policies);
        Map<Set<String>, PolicySimulation> policySimulationsByActions = mergePolicySimulationsByActions(policySimulationsByResources);
        return new HashSet<>(policySimulationsByActions.values());
    }

    private Map<Set<String>, PolicySimulation> mergePolicySimulationsByResources(Collection<Policy> policies) {
        Map<Set<String>, PolicySimulation> policySimulationsByResources = new HashMap<>();
        for (Policy policy : policies) {
            for (Statement statement : policy.getStatements()) {
                SortedSet<String> actions = getStatementActions(statement);
                SortedSet<String> resources = getStatementResources(statement);
                if (policySimulationsByResources.containsKey(resources)) {
                    policySimulationsByResources.get(resources).getActions().addAll(actions);
                } else {
                    policySimulationsByResources.put(resources, new PolicySimulation(resources, actions));
                }
            }
        }
        return policySimulationsByResources;
    }

    private Map<Set<String>, PolicySimulation> mergePolicySimulationsByActions(Map<Set<String>, PolicySimulation> policySimulationsByResources) {
        Map<Set<String>, PolicySimulation> policySimulationsByActions = new HashMap<>();
        for (PolicySimulation policySimulation : policySimulationsByResources.values()) {
            Set<String> actions = policySimulation.getActions();
            if (policySimulationsByActions.containsKey(actions)) {
                policySimulationsByActions.get(actions).getResources().addAll(policySimulation.getResources());
            } else {
                policySimulationsByActions.put(actions, policySimulation);
            }
        }
        return policySimulationsByActions;
    }

    /**
     * Returns resource file content as a string
     *
     * @param fileName resource file to read
     * @return resource content as a string
     * @throws IOException if unable to read file
     */
    String getResourceFileAsString(String fileName) throws IOException {
        try (InputStream is = AwsIamService.class.getClassLoader().getResourceAsStream(fileName)) {
            return inputStreamtoString(is);
        }
    }

    private String inputStreamtoString(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
}

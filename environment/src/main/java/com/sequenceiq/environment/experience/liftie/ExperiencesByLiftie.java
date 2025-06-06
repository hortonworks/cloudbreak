package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.environment.experience.liftie.LiftieIgnorableClusterStatuses.DELETED;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.experience.PolicyServiceName;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.Experience;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.config.LiftieWorkloadsConfig;
import com.sequenceiq.environment.experience.liftie.responses.LiftieClusterView;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

@Component
public class ExperiencesByLiftie implements Experience {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperiencesByLiftie.class);

    private static final String LIFTIE = "LIFTIE";

    private static final String KUBERNETES_XP = "Kubernetes Experience";

    private final ListClustersResponseValidator listClustersResponseValidator;

    private final LiftieApi liftieApi;

    private final Set<LiftieWorkload> workloads;

    public ExperiencesByLiftie(LiftieWorkloadsConfig workloadConfig, LiftieApi liftieApi,
            ListClustersResponseValidator listClustersResponseValidator) {
        this.listClustersResponseValidator = listClustersResponseValidator;
        this.liftieApi = liftieApi;
        workloads = identifyConfiguredWorkloads(workloadConfig);
    }

    @Override
    public Set<ExperienceCluster> getConnectedClustersForEnvironment(EnvironmentExperienceDto environment) {
        throwIfTrue(environment == null, () -> new IllegalArgumentException(EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!"));

        LOGGER.debug("Getting Liftie cluster list for environment '{}'", environment.getName());
        List<LiftieClusterView> clusterViews = getClusterViewsForWorkloads(environment.getName(), environment.getAccountId());
        Set<ExperienceCluster> result = clusterViews.stream()
                .filter(cv -> DELETED.isNotEqualTo(cv.getClusterStatus().getStatus()))
                .map(this::getExperienceCluster)
                .collect(Collectors.toSet());
        LOGGER.debug("Found {} non-deleted clusters total", result.size());
        return result;
    }

    @Override
    public void deleteConnectedExperiences(EnvironmentExperienceDto environment) {
        throwIfTrue(environment == null, () -> new IllegalArgumentException(EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!"));
        LOGGER.debug("Getting Liftie cluster list for environment '{}'", environment.getName());
        List<LiftieClusterView> clusterViews = getClusterViewsForWorkloads(environment.getName(), environment.getAccountId());
        LOGGER.debug("Starting Liftie clusters deletion for environment '{}'", environment.getName());
        clusterViews.stream()
                .filter(cluster -> LiftieIgnorableClusterStatuses.notContains(cluster.getClusterStatus().getStatus()))
                .forEach(cluster -> liftieApi.deleteCluster(cluster.getClusterId()));
        LOGGER.debug("Liftie clusters delete requests submitted for environment '{}'", environment.getName());
    }

    @Override
    @NotNull
    public Map<String, String> collectPolicy(EnvironmentExperienceDto environment) {
        String key = PolicyServiceName.LIFTIE.getPublicName();
        Map<String, String> result = new LinkedHashMap<>();
        try {
            ExperiencePolicyResponse fetchedPolicies = liftieApi.getPolicy(environment.getCloudPlatform());
            if (fetchedPolicies != null) {
                updatePolicyMapWithResult(fetchedPolicies, result, key);
            }
        } catch (ExperienceOperationFailedException eofe) {
            LOGGER.warn("Unable to fetch policy from Liftie  due to: " + eofe.getMessage(), eofe);
            result.put(key, "");
        }
        return result;
    }

    @Override
    public ExperienceSource getSource() {
        return ExperienceSource.LIFTIE;
    }

    private void updatePolicyMapWithResult(ExperiencePolicyResponse fetchedPolicies, Map<String, String> result, String xpPublicName) {
        if (fetchedPolicies.getAws() != null) {
            LOGGER.debug("Updating policy JSON map for '{}' for AWS.", xpPublicName);
            result.put(xpPublicName, fetchedPolicies.getAws().getPolicy());
        } else {
            result.put(xpPublicName, "");
        }
    }

    private Set<LiftieWorkload> identifyConfiguredWorkloads(LiftieWorkloadsConfig config) {
        Set<LiftieWorkload> workloads = config.getWorkloads();
        if (workloads.isEmpty()) {
            LOGGER.info("There are no configured Liftie Workload types in environment service! If you would like to check them, specify them" +
                    " in the experiences-config.yml!");
        } else {
            LOGGER.info("The following Liftie Workloads are configured: {}", workloads);
        }
        return workloads;
    }

    private List<LiftieClusterView> getClusterViewsForWorkloads(String environmentName, String accountId) {
        List<LiftieClusterView> clusterViews = new LinkedList<>();
        workloads.forEach(workload -> clusterViews.addAll(getClusterViewsForWorkload(environmentName, accountId, workload.getName())));
        return clusterViews;
    }

    private List<LiftieClusterView> getClusterViewsForWorkload(String environmentName, String tenant, String workload) {
        List<LiftieClusterView> clusterViews = new LinkedList<>();
        List<ListClustersResponse> clustersResponses = new LinkedList<>();
        ListClustersResponse first = liftieApi.listClusters(environmentName, tenant, workload, null);
        if (listClustersResponseValidator.isListClustersResponseEmpty(first)) {
            return clusterViews;
        }
        clustersResponses.add(first);
        if (first.getPage().getTotalPages() > 1) {
            int currentPage = first.getPage().getNumber() + 1;
            while (currentPage <= first.getPage().getTotalPages()) {
                ListClustersResponse response = liftieApi.listClusters(environmentName, tenant, workload, currentPage);
                if (!listClustersResponseValidator.isListClustersResponseEmpty(response)) {
                    clustersResponses.add(response);
                }
                currentPage++;
            }
        }
        for (ListClustersResponse clustersResponse : clustersResponses) {
            clusterViews.addAll(clustersResponse.getClusters().values());
        }
        return clusterViews;
    }

    private ExperienceCluster getExperienceCluster(LiftieClusterView cv) {
        return ExperienceCluster.builder()
                .withName(cv.getName())
                .withStatus(cv.getClusterStatus().getStatus())
                .withStatusReason(cv.getClusterStatus().getMessage())
                .withExperienceName(LIFTIE)
                .withPublicName(KUBERNETES_XP)
                .build();
    }

}

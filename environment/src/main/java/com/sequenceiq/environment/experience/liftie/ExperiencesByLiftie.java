package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;
import static com.sequenceiq.environment.experience.liftie.LiftieIgnorableClusterStatuses.DELETED;

import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.Experience;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.ClusterView;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class ExperiencesByLiftie implements Experience {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExperiencesByLiftie.class);

    private final ExperienceIndependentLiftieClusterWorkloadProvider workloadProvider;

    private final ListClustersResponseValidator listClustersResponseValidator;

    private final LiftieApi liftieApi;

    public ExperiencesByLiftie(ExperienceIndependentLiftieClusterWorkloadProvider workloadProvider, LiftieApi liftieApi,
            ListClustersResponseValidator listClustersResponseValidator) {
        this.listClustersResponseValidator = listClustersResponseValidator;
        this.workloadProvider = workloadProvider;
        this.liftieApi = liftieApi;
    }

    @Override
    public int getConnectedClusterCountForEnvironment(EnvironmentExperienceDto environment) {
        throwIfTrue(environment == null, () -> new IllegalArgumentException(EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!"));
        List<ClusterView> clusterViews = getClusterViewsForWorkloads(environment.getName(), environment.getAccountId());
        return countNotDeletedClusters(clusterViews);
    }

    @Override
    public void deleteConnectedExperiences(@NotNull EnvironmentExperienceDto environment) {
        throwIfTrue(environment == null, () -> new IllegalArgumentException(EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!"));
        LOGGER.debug("Getting Liftie cluster list for environment '{}'", environment.getName());
        List<ClusterView> clusterViews = getClusterViewsForWorkloads(environment.getName(), environment.getAccountId());
        LOGGER.debug("Starting Liftie clusters deletion for environment '{}'", environment.getName());
        clusterViews.stream()
                .filter(cluster -> LiftieIgnorableClusterStatuses.notContains(cluster.getClusterStatus().getStatus()))
                .forEach(cluster -> liftieApi.deleteCluster(cluster.getClusterId()));
        LOGGER.debug("Liftie clusters delete requests submitted for environment '{}'", environment.getName());
    }

    @Override
    public ExperienceSource getSource() {
        return ExperienceSource.LIFTIE;
    }

    private List<ClusterView> getClusterViewsForWorkloads(String environmentName, String accountId) {
        List<ClusterView> clusterViews = new LinkedList<>();
        workloadProvider.getWorkloadsLabels().forEach(workload -> clusterViews.addAll(getClusterViewsForWorkload(environmentName, accountId, workload)));
        return clusterViews;
    }

    private List<ClusterView> getClusterViewsForWorkload(String environmentName, String tenant, String workload) {
        List<ClusterView> clusterViews = new LinkedList<>();
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

    private int countNotDeletedClusters(List<ClusterView> clusterViews) {
        return Math.toIntExact(clusterViews
                .stream()
                .map(clusterView -> clusterView.getClusterStatus().getStatus())
                .filter(clusterStatus -> DELETED.isNotEqualTo(clusterStatus))
                .count());
    }

}

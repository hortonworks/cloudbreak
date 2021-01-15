package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.throwIfTrue;

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

    private final LiftieApi liftieApi;

    public ExperiencesByLiftie(LiftieApi liftieApi) {
        this.liftieApi = liftieApi;
    }

    @Override
    public int clusterCountForEnvironment(EnvironmentExperienceDto environment) {
        List<ClusterView> clusterViews = getClusterViews(environment.getName(), environment.getAccountId());
        return countNotDeletedClusters(clusterViews);
    }

    @Override
    public void deleteConnectedExperiences(@NotNull EnvironmentExperienceDto environment) {
        throwIfTrue(environment == null, () -> new IllegalArgumentException(EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!"));
        LOGGER.debug("Getting Liftie cluster list for environment '{}'", environment.getName());
        List<ClusterView> clusterViews = getClusterViews(environment.getName(), environment.getAccountId());
        LOGGER.debug("Starting Liftie clusters deletion for environment '{}'", environment.getName());
        clusterViews.stream()
                .filter(cv -> !"DELETED".equals(cv.getClusterStatus().getStatus()))
                .forEach(cv -> liftieApi.deleteCluster(cv.getClusterId()));
        LOGGER.debug("Liftie clusters delete requests submitted for environment '{}'", environment.getName());
    }

    @Override
    public ExperienceSource getSource() {
        return ExperienceSource.LIFTIE;
    }

    private List<ClusterView> getClusterViews(String environmentName, String tenant) {
        List<ClusterView> clusterViews = new LinkedList<>();
        List<ListClustersResponse> clustersResponses = new LinkedList<>();
        ListClustersResponse first = liftieApi.listClusters("gmeszaros-aws-1611222116", tenant, null);
        clustersResponses.add(first);
        if (first.getPage().getTotalPages() > 1) {
            int currentPage = first.getPage().getNumber() + 1;
            while (currentPage < first.getPage().getTotalPages()) {
                clustersResponses.add(liftieApi.listClusters("gmeszaros-aws-1611222116", tenant, currentPage));
                currentPage++;
            }
        }
        for (ListClustersResponse clustersResponse : clustersResponses) {
            clusterViews.addAll(clustersResponse.getClusters().values());
        }
        return clusterViews;
    }

    private int countNotDeletedClusters(List<ClusterView> clusterViews) {
        return Math.toIntExact(clusterViews.stream().filter(clusterView -> !"DELETED".equals(clusterView.getClusterStatus().getStatus())).count());
    }

}

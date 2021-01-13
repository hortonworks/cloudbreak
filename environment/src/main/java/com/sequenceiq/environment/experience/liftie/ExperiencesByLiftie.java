package com.sequenceiq.environment.experience.liftie;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.Experience;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.liftie.responses.ClusterView;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class ExperiencesByLiftie implements Experience {

    private final LiftieApi liftieApi;

    public ExperiencesByLiftie(LiftieApi liftieApi) {
        this.liftieApi = liftieApi;
    }

    @Override
    public boolean hasExistingClusterForEnvironment(EnvironmentExperienceDto environment) {
        List<ClusterView> clusterViews = getClusterViews(environment.getCrn(), environment.getAccountId());
        return countNotDeletedClusters(clusterViews) > 0;
    }

    @Override
    public void deleteConnectedExperiences(EnvironmentExperienceDto dto) {
        throw new NotImplementedException("deleteConnectedExperiences is not implemented yet");
    }

    @Override
    public ExperienceSource getSource() {
        return ExperienceSource.LIFTIE;
    }

    private List<ClusterView> getClusterViews(String environmentCrn, String tenant) {
        List<ClusterView> clusterViews = new LinkedList<>();
        ListClustersResponse first = liftieApi.listClusters(environmentCrn, tenant, null, null);
        if (first.getPage().getTotalPages() > 1) {
            List<ListClustersResponse> clustersResponses = new LinkedList<>();
            clustersResponses.add(first);
            int currentPage = first.getPage().getNumber() + 1;
            while (currentPage < first.getPage().getTotalPages()) {
                clustersResponses.add(liftieApi.listClusters(environmentCrn, tenant, null, currentPage));
                currentPage++;
            }
            for (ListClustersResponse clustersResponse : clustersResponses) {
                clusterViews.addAll(clustersResponse.getClusters().values());
            }
        } else {
            clusterViews.addAll(first.getClusters().values());
        }
        return clusterViews;
    }

    private long countNotDeletedClusters(List<ClusterView> clusterViews) {
        return clusterViews.stream().filter(clusterView -> !"DELETED".equals(clusterView.getClusterStatus().getStatus())).count();
    }

}

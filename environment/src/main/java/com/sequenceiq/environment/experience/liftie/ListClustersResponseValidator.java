package com.sequenceiq.environment.experience.liftie;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class ListClustersResponseValidator {

    public boolean isListClustersResponseEmpty(ListClustersResponse response) {
        return response == null || isClustersAreEmpty(response) || isPagesAreEmpty(response);
    }

    private boolean isClustersAreEmpty(ListClustersResponse response) {
        return MapUtils.isEmpty(response.getClusters());
    }

    private boolean isPagesAreEmpty(ListClustersResponse response) {
        return response.getPage() == null ||
                (response.getPage().getTotalPages() == null || response.getPage().getTotalPages() == 0);
    }

}

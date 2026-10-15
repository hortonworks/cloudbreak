package com.sequenceiq.environment.experience.liftie;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

@Component
public class ListClustersResponseValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListClustersResponseValidator.class);

    public boolean isListClustersResponseEmpty(ListClustersResponse response) {
        return isNull(response) || isClustersAreEmpty(response) || isPagesAreEmpty(response);
    }

    private boolean isClustersAreEmpty(ListClustersResponse response) {
        boolean clustersMapIsEmpty = MapUtils.isEmpty(response.getClusters());
        if (clustersMapIsEmpty) {
            LOGGER.info("The map of clusters within the {} was empty or null!", ListClustersResponse.class.getSimpleName());
        }
        return clustersMapIsEmpty;
    }

    private boolean isPagesAreEmpty(ListClustersResponse response) {
        boolean pagesAreEmpty = response.getPage() == null || response.getPage().getTotalPages() == null || response.getPage().getTotalPages() == 0;
        if (pagesAreEmpty) {
            LOGGER.info("{} has not contained any element!", ListClustersResponse.class.getSimpleName());
        }
        return pagesAreEmpty;
    }

    private boolean isNull(ListClustersResponse response) {
        if (response == null) {
            LOGGER.info("{} was null!", ListClustersResponse.class.getSimpleName());
            return true;
        }
        return false;
    }

}

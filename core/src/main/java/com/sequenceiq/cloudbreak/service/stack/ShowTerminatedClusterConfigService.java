package com.sequenceiq.cloudbreak.service.stack;

import java.time.temporal.TemporalAmount;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ShowTerminatedClustersPreferences;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.TimeService;

@Service
public class ShowTerminatedClusterConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTerminatedClusterConfigService.class);

    @Inject
    private ShowTerminatedClusterParameterService showTerminatedClusterParameterService;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private TimeService timeService;

    public ShowTerminatedConfig get() {
        ShowTerminatedClustersPreferences showTerminatedClustersPreferences = userProfileService.getOrCreateForLoggedInUser().getShowTerminatedClustersPreferences();
        if (showTerminatedClustersPreferences.isActive()) {
            return new ShowTerminatedConfig(
                    showTerminatedClustersPreferences.isActive(),
                    computeShowAfterTimestampMillisecs(showTerminatedClustersPreferences.getTimeout())
            );
        }
        return new ShowTerminatedConfig(
                showTerminatedClusterParameterService.getShowTerminatedOn(),
                computeShowAfterTimestampMillisecs(showTerminatedClusterParameterService.getTimeout())
        );
    }

    private Long computeShowAfterTimestampMillisecs(TemporalAmount duration) {
        return timeService.nowMinus(duration).toEpochMilli();
    }

    public static class ShowTerminatedConfig {

        private final Boolean active;

        private final Long showTerminatedAfterMillisecs;

        private ShowTerminatedConfig(Boolean active, Long showTerminatedAfterMillisecs) {
            this.active = active;
            this.showTerminatedAfterMillisecs = showTerminatedAfterMillisecs;
        }

        public Boolean isActive() {
            return active;
        }

        public Long showAfterMillisecs() {
            return showTerminatedAfterMillisecs;
        }
    }
}

package com.sequenceiq.cloudbreak.service.stack;

import java.time.Duration;
import java.time.temporal.TemporalAmount;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ShowTerminatedClustersPreferences;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@Service
public class ShowTerminatedClusterConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTerminatedClusterConfigService.class);

    private ShowTerminatedClustersConfig defaultShowTerminatedClustersConfig;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private Clock clock;

    @Value("${cb.show.terminated.clusters.active:false}")
    private Boolean showTerminatedOn;

    @Value("${cb.show.terminated.clusters.days:7}")
    private Long timeoutDays;

    @Value("${cb.show.terminated.clusters.hours:0}")
    private Long timeoutHours;

    @Value("${cb.show.terminated.clusters.minutes:0}")
    private Long timeoutMinutes;

    @PostConstruct
    void init() {
        Duration timeout = Duration
                .ofMinutes(getValue(timeoutMinutes))
                .plusHours(getValue(timeoutHours))
                .plusDays(getValue(timeoutDays));
        defaultShowTerminatedClustersConfig = new ShowTerminatedClustersConfig(showTerminatedOn, timeout, false);
    }

    private long getValue(Long value) {
        return value != null ? value : 0;
    }

    public ShowTerminatedClustersAfterConfig get() {
        ShowTerminatedClustersConfig config = getConfig();
        return new ShowTerminatedClustersAfterConfig(
                config.isActive(),
                computeShowAfterTimestampMillisecs(config.getTimeout())
        );
    }

    public ShowTerminatedClustersConfig getConfig() {
        ShowTerminatedClustersPreferences showTerminatedClustersPreferences =
                userProfileService.getOrCreateForLoggedInUser().getShowTerminatedClustersPreferences();
        if (showTerminatedClustersPreferences == null) {
            return defaultShowTerminatedClustersConfig;
        }
        return new ShowTerminatedClustersConfig(showTerminatedClustersPreferences.isActive(), showTerminatedClustersPreferences.getTimeout(), true);
    }

    public void set(ShowTerminatedClustersConfig newConfig) {
        UserProfile userProfile = userProfileService.getOrCreateForLoggedInUser();
        ShowTerminatedClustersPreferences storedPreferences = userProfile.getShowTerminatedClustersPreferences();
        if (storedPreferences == null) {
            storedPreferences = initStoredPreferences(userProfile);
        }
        storedPreferences.setActive(newConfig.isActive());
        if (newConfig.getTimeout() != null) {
            storedPreferences.setTimeout(newConfig.getTimeout());
        }
        userProfileService.save(userProfile);
    }

    public void delete() {
        UserProfile userProfile = userProfileService.getOrCreateForLoggedInUser();
        userProfile.setShowTerminatedClustersPreferences(null);
        userProfileService.save(userProfile);
    }

    private Long computeShowAfterTimestampMillisecs(TemporalAmount duration) {
        return clock.nowMinus(duration).toEpochMilli();
    }

    private ShowTerminatedClustersPreferences initStoredPreferences(UserProfile userProfile) {
        ShowTerminatedClustersPreferences storedPreferences;
        storedPreferences = new ShowTerminatedClustersPreferences();
        storedPreferences.setTimeout(defaultShowTerminatedClustersConfig.getTimeout());
        userProfile.setShowTerminatedClustersPreferences(storedPreferences);
        return storedPreferences;
    }

    public static class ShowTerminatedClustersAfterConfig {

        private final Boolean active;

        private final Long showTerminatedAfterMillisecs;

        private ShowTerminatedClustersAfterConfig(Boolean active, Long showTerminatedAfterMillisecs) {
            this.active = active;
            this.showTerminatedAfterMillisecs = showTerminatedAfterMillisecs;
        }

        public Boolean isActive() {
            return active;
        }

        Long showAfterMillisecs() {
            return showTerminatedAfterMillisecs;
        }
    }
}

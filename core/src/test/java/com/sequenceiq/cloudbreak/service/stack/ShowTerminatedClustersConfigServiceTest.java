package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.ShowTerminatedClustersPreferences;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService.ShowTerminatedClustersAfterConfig;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;

@ExtendWith(MockitoExtension.class)
class ShowTerminatedClustersConfigServiceTest {

    private static final Duration DURATION_D1_H2_M3 = Duration.ofDays(1).plusHours(2).plusMinutes(3);

    private static final Duration DURATION_D4_H5_M6 = Duration.ofDays(4).plusHours(5).plusMinutes(6);

    private static final long DEFAULT_TIMEOUT_DAYS = 9;

    private static final long DEFAULT_TIMEOUT_HOURS = 8;

    private static final long DEFAULT_TIMEOUT_MINUTES = 7;

    private static final Duration DEFAULT_DURATION = Duration
            .ofDays(DEFAULT_TIMEOUT_DAYS)
            .plusHours(DEFAULT_TIMEOUT_HOURS)
            .plusMinutes(DEFAULT_TIMEOUT_MINUTES);

    @Mock
    private Clock clock;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private ShowTerminatedClusterConfigService underTest;

    private void init(Boolean showTerminatedOn) {
        ReflectionTestUtils.setField(underTest, "showTerminatedOn", showTerminatedOn);
        ReflectionTestUtils.setField(underTest, "timeoutDays", DEFAULT_TIMEOUT_DAYS);
        ReflectionTestUtils.setField(underTest, "timeoutHours", DEFAULT_TIMEOUT_HOURS);
        ReflectionTestUtils.setField(underTest, "timeoutMinutes", DEFAULT_TIMEOUT_MINUTES);
        underTest.init();
    }

    @Test
    void testGetWhenUserHasPreference() {
        init(false);
        when(clock.nowMinus(DEFAULT_DURATION)).thenReturn(Instant.ofEpochSecond(0));

        ShowTerminatedClustersAfterConfig showTerminatedClustersConfig = underTest.get();

        assertFalse(showTerminatedClustersConfig.isActive());
        assertEquals(Long.valueOf(0), showTerminatedClustersConfig.showAfterMillisecs());
        verify(userProfileService, times(0)).getOrCreateForLoggedInUser();
    }

    @Test
    void testGetWhenUserHasNoPreference() {
        init(false);
        when(clock.nowMinus(any())).thenReturn(Instant.ofEpochSecond(0));

        ShowTerminatedClustersAfterConfig showTerminatedClustersAfterConfig = underTest.get();

        assertFalse(showTerminatedClustersAfterConfig.isActive());
        assertEquals(0L, showTerminatedClustersAfterConfig.showAfterMillisecs().longValue());
        verify(clock).nowMinus(any());
        verify(userProfileService, times(0)).getOrCreateForLoggedInUser();
    }

    @Test
    void testGetConfigWhenUserHasPreference() {
        init(false);

        ShowTerminatedClustersConfig showTerminatedClustersConfig = underTest.getConfig();

        assertFalse(showTerminatedClustersConfig.isActive());
        assertEquals("GLOBAL", showTerminatedClustersConfig.getSource().name());
        assertEquals(DEFAULT_TIMEOUT_DAYS, showTerminatedClustersConfig.getTimeout().toDaysPart());
        assertEquals(DEFAULT_TIMEOUT_HOURS, showTerminatedClustersConfig.getTimeout().toHoursPart());
        assertEquals(DEFAULT_TIMEOUT_MINUTES, showTerminatedClustersConfig.getTimeout().toMinutesPart());
        verify(userProfileService, times(0)).getOrCreateForLoggedInUser();
    }

    @Test
    void testGetConfigWhenUserHasNoPreference() {
        init(false);

        ShowTerminatedClustersConfig showTerminatedClustersConfig = underTest.getConfig();

        assertFalse(showTerminatedClustersConfig.isActive());
        assertEquals("GLOBAL", showTerminatedClustersConfig.getSource().name());
        assertEquals(DEFAULT_TIMEOUT_DAYS, showTerminatedClustersConfig.getTimeout().toDaysPart());
        assertEquals(DEFAULT_TIMEOUT_HOURS, showTerminatedClustersConfig.getTimeout().toHoursPart());
        assertEquals(DEFAULT_TIMEOUT_MINUTES, showTerminatedClustersConfig.getTimeout().toMinutesPart());
        verify(userProfileService, times(0)).getOrCreateForLoggedInUser();
    }

    @Test
    void testSetWhenUserHasNoPreference() {
        init(false);
        when(userProfileService.getOrCreateForLoggedInUser()).thenReturn(new UserProfileBuilder().withNullClusterPrefs().build());

        underTest.set(new ShowTerminatedClustersConfig(true, DURATION_D1_H2_M3, true));

        ArgumentCaptor<UserProfile> savedUserProfile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).save(savedUserProfile.capture());
        assertNotNull(savedUserProfile.getValue().getShowTerminatedClustersPreferences());
        assertTrue(savedUserProfile.getValue().getShowTerminatedClustersPreferences().isActive());
        assertEquals(1, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toDaysPart());
        assertEquals(2, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toHoursPart());
        assertEquals(3, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toMinutesPart());
    }

    @Test
    void testSetWhenUserHasPreference() {
        init(false);
        when(userProfileService.getOrCreateForLoggedInUser()).thenReturn(new UserProfileBuilder().withShowClusterPrefs(false, DEFAULT_DURATION).build());

        underTest.set(new ShowTerminatedClustersConfig(true, DURATION_D1_H2_M3, true));

        ArgumentCaptor<UserProfile> savedUserProfile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).save(savedUserProfile.capture());
        assertNotNull(savedUserProfile.getValue().getShowTerminatedClustersPreferences());
        assertTrue(savedUserProfile.getValue().getShowTerminatedClustersPreferences().isActive());
        assertEquals(1, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toDaysPart());
        assertEquals(2, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toHoursPart());
        assertEquals(3, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toMinutesPart());
    }

    @Test
    void testSetWhenTimeoutIsNotSetAndNoUserPreference() {
        init(false);
        when(userProfileService.getOrCreateForLoggedInUser()).thenReturn(new UserProfileBuilder().withNullClusterPrefs().build());

        underTest.set(new ShowTerminatedClustersConfig(true, null, true));

        ArgumentCaptor<UserProfile> savedUserProfile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).save(savedUserProfile.capture());
        assertNotNull(savedUserProfile.getValue().getShowTerminatedClustersPreferences());
        assertTrue(savedUserProfile.getValue().getShowTerminatedClustersPreferences().isActive());
        assertEquals(9, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toDaysPart());
        assertEquals(8, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toHoursPart());
        assertEquals(7, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toMinutesPart());
    }

    @Test
    void testSetWhenTimeoutIsNotSet() {
        init(true);
        when(userProfileService.getOrCreateForLoggedInUser()).thenReturn(new UserProfileBuilder().withShowClusterPrefs(true, DURATION_D4_H5_M6).build());

        underTest.set(new ShowTerminatedClustersConfig(false, null, true));

        ArgumentCaptor<UserProfile> savedUserProfile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).save(savedUserProfile.capture());
        assertNotNull(savedUserProfile.getValue().getShowTerminatedClustersPreferences());
        assertFalse(savedUserProfile.getValue().getShowTerminatedClustersPreferences().isActive());
        assertEquals(4, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toDaysPart());
        assertEquals(5, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toHoursPart());
        assertEquals(6, savedUserProfile.getValue().getShowTerminatedClustersPreferences().getTimeout().toMinutesPart());
    }

    @Test
    void testDeleteWhenUserHasNoPreference() {
        init(false);
        when(userProfileService.getOrCreateForLoggedInUser()).thenReturn(new UserProfileBuilder().withNullClusterPrefs().build());

        underTest.delete();

        ArgumentCaptor<UserProfile> savedUserProfile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).save(savedUserProfile.capture());
        assertNull(savedUserProfile.getValue().getShowTerminatedClustersPreferences());
    }

    @Test
    void testDeleteWhenUserHasPreference() {
        init(false);
        when(userProfileService.getOrCreateForLoggedInUser()).thenReturn(new UserProfileBuilder().withShowClusterPrefs(false, DEFAULT_DURATION).build());

        underTest.delete();

        ArgumentCaptor<UserProfile> savedUserProfile = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).save(savedUserProfile.capture());
        assertNull(savedUserProfile.getValue().getShowTerminatedClustersPreferences());
    }

    private static class UserProfileBuilder {
        private final UserProfile userProfile = new UserProfile();

        private UserProfileBuilder withShowClusterPrefs(boolean active, Duration timeout) {
            userProfile.setShowTerminatedClustersPreferences(
                    new ShowTerminatedClustersPreferences(active, timeout.toMillis())
            );
            return this;
        }

        private UserProfileBuilder withNullClusterPrefs() {
            userProfile.setShowTerminatedClustersPreferences(null);
            return this;
        }

        private UserProfile build() {
            return userProfile;
        }
    }
}

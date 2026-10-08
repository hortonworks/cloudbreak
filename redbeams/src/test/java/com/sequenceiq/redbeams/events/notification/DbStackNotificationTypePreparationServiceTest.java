package com.sequenceiq.redbeams.events.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

class DbStackNotificationTypePreparationServiceTest {

    private final DbStackNotificationTypePreparationService underTest = new DbStackNotificationTypePreparationService();

    @Test
    void shouldRequireNotificationForLowStorageEvent() {
        assertThat(underTest.isNotificationRequiredByStackStatus(ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW)).isTrue();
    }

    @Test
    void shouldNotRequireNotificationForUnrelatedEvent() {
        assertThat(underTest.isNotificationRequiredByStackStatus(
                ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_REDBEAMS_STARTED)).isFalse();
    }
}

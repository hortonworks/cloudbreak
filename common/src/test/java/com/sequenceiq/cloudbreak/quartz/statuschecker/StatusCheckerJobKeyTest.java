package com.sequenceiq.cloudbreak.quartz.statuschecker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;

@ExtendWith(MockitoExtension.class)
class StatusCheckerJobKeyTest {

    private static final String GROUP_NAME = "groupName";

    private static final String SUBGROUP_NAME = "subgroupName";

    private static final String RESOURCE_ID = "1";

    private static final String JOB_NAME = RESOURCE_ID + '-' + SUBGROUP_NAME;

    @Test
    void toQuartzJobKeyWithoutSubGroup() {
        JobKey result = new StatusCheckerJobKey(RESOURCE_ID, GROUP_NAME).toQuartzJobKey();

        assertThat(result.getName()).isEqualTo(RESOURCE_ID);
        assertThat(result.getGroup()).isEqualTo(GROUP_NAME);
    }

    @Test
    void toQuartzJobKeyWithSubGroup() {
        JobKey result = new StatusCheckerJobKey(RESOURCE_ID, GROUP_NAME, SUBGROUP_NAME).toQuartzJobKey();

        assertThat(result.getName()).isEqualTo(JOB_NAME);
        assertThat(result.getGroup()).isEqualTo(GROUP_NAME);
    }

    @Test
    void fromQuartzJobKeyWithoutSubgroup() {
        StatusCheckerJobKey result = StatusCheckerJobKey.fromQuartzJobKey(JobKey.jobKey(RESOURCE_ID, GROUP_NAME));

        assertThat(result.resourceId()).isEqualTo(RESOURCE_ID);
        assertThat(result.groupName()).isEqualTo(GROUP_NAME);
        assertThat(result.subGroupName()).isNull();
    }

    @Test
    void fromQuartzJobKeyWithSubgroup() {
        StatusCheckerJobKey result = StatusCheckerJobKey.fromQuartzJobKey(JobKey.jobKey(JOB_NAME, GROUP_NAME));

        assertThat(result.resourceId()).isEqualTo(RESOURCE_ID);
        assertThat(result.groupName()).isEqualTo(GROUP_NAME);
        assertThat(result.subGroupName()).isEqualTo(SUBGROUP_NAME);
    }

    @ParameterizedTest
    @MethodSource("hasConflictWithSource")
    void hasConflictWith(String resourceId, String groupName, String subGroupName, boolean conflict) {
        StatusCheckerJobKey statusCheckerJobKey = new StatusCheckerJobKey(RESOURCE_ID, GROUP_NAME, SUBGROUP_NAME);
        StatusCheckerJobKey otherStatusCheckerJobKey = new StatusCheckerJobKey(resourceId, groupName, subGroupName);

        boolean result = statusCheckerJobKey.hasConflictWith(otherStatusCheckerJobKey);

        assertThat(result).isEqualTo(conflict);
    }

    public static Stream<Arguments> hasConflictWithSource() {
        return Stream.of(
                Arguments.of("2", "g", "sg", false),
                Arguments.of(RESOURCE_ID, "g", "sg", false),
                Arguments.of("2", GROUP_NAME, "sg", false),
                Arguments.of(RESOURCE_ID, GROUP_NAME, "sg", true),
                Arguments.of(RESOURCE_ID, GROUP_NAME, SUBGROUP_NAME, true)
        );
    }

}

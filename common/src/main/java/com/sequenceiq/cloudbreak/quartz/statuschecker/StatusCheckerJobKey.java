package com.sequenceiq.cloudbreak.quartz.statuschecker;

import java.util.Objects;

import org.quartz.JobKey;

/**
 * Quarts jobs have keys in a group + name format, but to have multiple jobs in the same group running for the same stack (which we identify by resourceId
 * as job name) a subgroup abstraction is needed too, so these jobs do not override eachother. To achieve this, the quartz job name can be utilized in a
 * "resourceId-subgroup" format. To generalize this format, the {@link StatusCheckerJobKey} class was created to parse to and from the quartz {@link JobKey}
 * format. Example in params that would be translated to {@code new JobKey("1-USER_DATA_CCMV2_SETUP", "existing-stack-patcher-jobs")}
 * @param resourceId e.g. 1 (stack id)
 * @param groupName e.g. existing-stack-patcher-jobs
 * @param subGroupName e.g. USER_DATA_CCMV2_SETUP (stack patch type)
 */
public record StatusCheckerJobKey(String resourceId, String groupName, String subGroupName) {

    public StatusCheckerJobKey(String resourceId, String groupName) {
        this(resourceId, groupName, null);
    }

    public JobKey toQuartzJobKey() {
        String name = subGroupName != null
                ? String.format("%s-%s", resourceId, subGroupName)
                : resourceId;
        return new JobKey(name, groupName);
    }

    public static StatusCheckerJobKey fromQuartzJobKey(JobKey jobKey) {
        String[] nameParts = jobKey.getName().split("-");
        return new StatusCheckerJobKey(nameParts[0], jobKey.getGroup(), nameParts.length > 1 ? nameParts[1] : null);
    }

    public boolean hasConflictWith(StatusCheckerJobKey other) {
        return Objects.equals(resourceId, other.resourceId) && Objects.equals(groupName, other.groupName);
    }

    @Override
    public String toString() {
        return "StatusCheckerJobKey{" +
                "resourceId='" + resourceId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", subGroupName='" + subGroupName + '\'' +
                '}';
    }
}

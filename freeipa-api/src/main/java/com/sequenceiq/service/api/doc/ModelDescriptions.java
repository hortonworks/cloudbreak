package com.sequenceiq.service.api.doc;

public class ModelDescriptions {
    public static final String ID = "id of the resource";
    public static final String CRN = "CRN of the resource";
    public static final String NAME = "name of the resource";
    public static final String DESCRIPTION = "description of the resource";
    public static final String ENVIRONMENT_NAME = "The name of the environment";
    public static final String ENVIRONMENT_CRN = "CRN of the environment";
    public static final String PARENT_ENVIRONMENT_CRN = "CRN of the parent environment";
    public static final String CHILD_ENVIRONMENT_CRN = "CRN of the child environment";
    public static final String CLUSTER_CRN = "CRN of the cluster";
    public static final String INSTANCE_ID = "ID of the instance";
    public static final String FORCE_REBOOT = "Reboot instance regardless of status";
    public static final String FORCE_REPAIR = "Repair instance regardless of status";
    public static final String USER_CRN = "CRN of a user. It is optional as it will be available in \"x-cdp-actor-crn\" header in the request.";
    public static final String ISSUE = "Issue number or JIRA ticket number related to this diagnostic collection request.";
    public static final String LABELS = "With labels you can filter what kind of logs you'd like to collect.";
    public static final String START_TIME = "Start time for the time interval of the diagnostic collection request.";
    public static final String END_TIME = "END time for the time interval of the diagnostic collection request.";
    public static final String DESTINATION = "Destination for the diagnostic collection request.";
    public static final String GROUPS = "Groups to check for.";
    public static final String USERS = "Users to check for.";
    public static final String GROUP = "Group to check for.";
    public static final String RESULT = "Result of the check.";

    private ModelDescriptions() {
    }
}

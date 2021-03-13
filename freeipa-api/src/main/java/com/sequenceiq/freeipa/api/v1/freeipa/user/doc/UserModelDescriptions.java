package com.sequenceiq.freeipa.api.v1.freeipa.user.doc;

public class UserModelDescriptions {

    public static final String SYNC_OPERATION = "Operation type";
    public static final String USERSYNC_ENVIRONMENT_CRNS = "Optional environment crns to sync";
    public static final String USERSYNC_USER_CRNS = "Optional user crns to sync";
    public static final String USERSYNC_MACHINEUSER_CRNS = "Optional machine user crns to sync";
    public static final String DELETED_WORKLOAD_USERS = "Optional workload user name to be deleted. Currently only 1 deleted user is supported. When this" +
            " is included, there should also be exactly 1 crn in the machineUsers or users set that corresponds to the deleted workload user.";
    public static final String USERSYNC_ID = "User synchronization operation id";
    public static final String USERSYNC_STATUS = "User synchronization operation status";
    public static final String USERSYNC_STARTTIME = "User synchronization operation start time";
    public static final String USERSYNC_ENDTIME = "User synchronization operation end time";
    public static final String USERSYNC_ERROR = "error information about operation failure";
    public static final String USERSYNC_ACCOUNT_ID = "The id of the account to run sync on";
    public static final String SUCCESS_ENVIRONMENTS = "details about environments where operation succeeded";
    public static final String FAILURE_ENVIRONMENTS = "details about environments where operation failed";
    public static final String USER_PASSWORD = "the user's password";
    public static final String USERSYNC_STATE = "state of user synchronization";
    public static final String FORCE_WORKLOAD_CREDENTIALS_UPDATE = "when synchronizing a user, update workload credentials " +
            "whether or not they have changed";

    private UserModelDescriptions() {
    }
}

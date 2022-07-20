package com.sequenceiq.common.api.command.doc;

public class RemoteCommandsExecutionDescription {

    public static final String COMMAND = "Command that will be executed on hosts remotely.";
    public static final String RESULTS = "Result of the remotly exeecuted commands - per host.";
    public static final String HOSTS = "Host (fqdn) filter, use it to run remote commands on only specific hosts.";
    public static final String HOST_GROUPS = "Host groups (instance groups), used it to run remote commands only those " +
            "hosts that are included the specific host groups.";

    private RemoteCommandsExecutionDescription() {
    }
}

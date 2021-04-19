package com.sequenceiq.cloudbreak.cm.polling;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class ClouderaManagerSyncCommandPollerObject extends ClouderaManagerCommandPollerObject {

    private final String commandName;

    public ClouderaManagerSyncCommandPollerObject(Stack stack, ApiClient apiClient, Integer id, String commandName) {
        super(stack, apiClient, id);
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}

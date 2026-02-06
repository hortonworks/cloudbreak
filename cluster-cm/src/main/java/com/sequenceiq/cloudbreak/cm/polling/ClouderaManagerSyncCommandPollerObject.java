package com.sequenceiq.cloudbreak.cm.polling;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerSyncCommandPollerObject extends ClouderaManagerCommandPollerObject {

    private final String commandName;

    public ClouderaManagerSyncCommandPollerObject(StackDtoDelegate stack, ApiClient apiClient, Long id, String commandName) {
        super(stack, apiClient, id);
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}

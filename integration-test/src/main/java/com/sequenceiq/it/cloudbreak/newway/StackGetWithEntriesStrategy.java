package com.sequenceiq.it.cloudbreak.newway;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponseEntries;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackGetWithEntriesStrategy implements Strategy {
    private final Set<StackResponseEntries> entries;

    private StackGetWithEntriesStrategy(Set<StackResponseEntries> entries) {
        this.entries = entries;
    }

    public static StackGetWithEntriesStrategy create(Set<StackResponseEntries> entries) {
        Preconditions.checkNotNull(entries, "entries must be set");
        return new StackGetWithEntriesStrategy(entries);
    }

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        Log.log(" get stack " + stackEntity.getName());
        stackEntity.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .getByNameInWorkspace(client.getWorkspaceId(), stackEntity.getName(), entries.stream()
                                .map(StackResponseEntries::getEntryName).collect(Collectors.toSet())));
        Log.logJSON(" stack get response: ", stackEntity.getResponse());
    }
}

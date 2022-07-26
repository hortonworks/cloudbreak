package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.remote.RemoteFreeIpaCommandAction;
import com.sequenceiq.it.cloudbreak.action.remote.RemoteStackCommandAction;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRemoteTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackRemoteTestDto;

@Service
public class RemoteTestClient {
    public Action<FreeIpaRemoteTestDto, FreeIpaClient> executeCommandOnFreeIpa() {
        return new RemoteFreeIpaCommandAction();
    }

    public Action<StackRemoteTestDto, CloudbreakClient> executeCommandOnStack() {
        return new RemoteStackCommandAction();
    }
}

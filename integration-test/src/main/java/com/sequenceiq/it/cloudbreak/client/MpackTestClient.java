package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.v4.mpack.MpackCreateAction;
import com.sequenceiq.it.cloudbreak.action.v4.mpack.MpackDeleteAction;
import com.sequenceiq.it.cloudbreak.action.v4.mpack.MpackListAction;
import com.sequenceiq.it.cloudbreak.dto.mpack.MPackTestDto;

@Service
public class MpackTestClient {

    public Action<MPackTestDto, CloudbreakClient> createV4() {
        return new MpackCreateAction();
    }

    public Action<MPackTestDto, CloudbreakClient> listV4() {
        return new MpackListAction();
    }

    public Action<MPackTestDto, CloudbreakClient> deleteV4() {
        return new MpackDeleteAction();
    }

}

package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.v4.mpack.MpackCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.mpack.MpackDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.v4.mpack.MpackListAction;
import com.sequenceiq.it.cloudbreak.newway.dto.mpack.MPackTestDto;

@Service
public class MpackTestClient {

    public Action<MPackTestDto> createV4() {
        return new MpackCreateAction();
    }

    public Action<MPackTestDto> listV4() {
        return new MpackListAction();
    }

    public Action<MPackTestDto> deleteV4() {
        return new MpackDeleteAction();
    }

}

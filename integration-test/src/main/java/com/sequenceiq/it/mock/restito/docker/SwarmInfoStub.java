package com.sequenceiq.it.mock.restito.docker;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.docker.model.Info;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class SwarmInfoStub extends RestitoStub {

    public static final String PATH = "/info";
    private final int serverNumber;

    public SwarmInfoStub(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    private Info createInfo() {
        Info info = new Info();
        List<Object> statusList = createStatusList();
        info.setDriverStatuses(statusList);
        return info;
    }

    private List<Object> createStatusList() {
        List<Object> statusList = new ArrayList<>();
        for (int i = 1; i <= serverNumber; i++) {
            List<String> ipList = new ArrayList<>();
            ipList.add("server");
            ipList.add("192.168.1." + i);
            statusList.add(ipList);
        }
        return statusList;
    }

    @Override
    public Condition getCondition() {
        return Condition.get(SWARM_API_ROOT + PATH);
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.contentType("application/json"), Action.stringContent(convertToJson(createInfo())));
    }
}

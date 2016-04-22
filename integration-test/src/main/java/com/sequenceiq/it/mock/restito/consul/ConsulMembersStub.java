package com.sequenceiq.it.mock.restito.consul;

import java.util.ArrayList;
import java.util.List;

import com.ecwid.consul.v1.agent.model.Member;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class ConsulMembersStub extends RestitoStub {

    public static final String PATH = "/agent/members";
    private static final int ALIVE_STATUS = 1;
    private int serverNumber;

    public ConsulMembersStub(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    private List<Member> createMembers() {
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= serverNumber; i++) {
            Member member = new Member();
            member.setAddress("192.168.1." + i);
            member.setStatus(ALIVE_STATUS);
            member.setName("consul" + i);
            members.add(member);
        }
        return members;
    }

    @Override
    public Condition getCondition() {
        return Condition.get(CONSUL_API_ROOT + PATH);
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.contentType("application/json"), Action.stringContent(convertToJson(createMembers())));
    }

    @Override
    public void setExpectedTimes(int expectedTimes) {
        super.setExpectedTimes(expectedTimes);
    }
}

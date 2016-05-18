package com.sequenceiq.it.spark.consul;

import java.util.ArrayList;
import java.util.List;

import com.ecwid.consul.v1.agent.model.Member;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class ConsulMemberResponse extends ITResponse {
    private static final int ALIVE_STATUS = 1;
    private int serverNumber;

    public ConsulMemberResponse(int serverNumber) {
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
    public Object handle(Request request, Response response) throws Exception {
        return createMembers();
    }
}

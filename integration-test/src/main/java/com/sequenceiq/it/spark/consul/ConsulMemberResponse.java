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
        for (int i = 0; i <= serverNumber / 254; i++) {
            int subAddress = Integer.min(254, serverNumber - i * 254);
            for (int j = 1; j <= subAddress; j++) {
                Member member = new Member();
                member.setAddress("192.168." + i + "." + j);
                member.setStatus(ALIVE_STATUS);
                member.setName("consul-" + i + "-" + j);
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        return createMembers();
    }
}

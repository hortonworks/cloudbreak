package com.sequenceiq.it.spark.consul

import java.util.ArrayList

import com.ecwid.consul.v1.agent.model.Member
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class ConsulMemberResponse(private val serverNumber: Int) : ITResponse() {

    private fun createMembers(): List<Member> {
        val members = ArrayList<Member>()
        for (i in 0..serverNumber / 254) {
            val subAddress = Integer.min(254, serverNumber - i * 254)
            for (j in 1..subAddress) {
                val member = Member()
                member.address = "192.168.$i.$j"
                member.status = ALIVE_STATUS
                member.name = "consul-$i-$j"
                members.add(member)
            }
        }
        return members
    }

    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        return createMembers()
    }

    companion object {
        private val ALIVE_STATUS = 1
    }
}

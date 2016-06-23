package com.sequenceiq.it.spark.consul

import java.util.ArrayList

import org.apache.commons.codec.binary.Base64

import com.ecwid.consul.v1.kv.model.GetValue
import com.sequenceiq.it.spark.ITResponse

import spark.Request
import spark.Response

class ConsulKeyValueGetResponse : ITResponse() {
    @Throws(Exception::class)
    override fun handle(request: Request, response: Response): Any {
        val getValueList = ArrayList<GetValue>()
        val getValue = GetValue()
        getValue.value = Base64.encodeBase64String("FINISHED".toByteArray())
        getValueList.add(getValue)
        return getValueList
    }
}

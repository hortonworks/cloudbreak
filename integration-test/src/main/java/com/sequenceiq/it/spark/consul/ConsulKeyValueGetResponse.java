package com.sequenceiq.it.spark.consul;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.ecwid.consul.v1.kv.model.GetValue;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class ConsulKeyValueGetResponse extends ITResponse {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        List<GetValue> getValueList = new ArrayList<>();
        GetValue getValue = new GetValue();
        getValue.setValue(Base64.encodeBase64String("FINISHED".getBytes()));
        getValueList.add(getValue);
        return getValueList;
    }
}

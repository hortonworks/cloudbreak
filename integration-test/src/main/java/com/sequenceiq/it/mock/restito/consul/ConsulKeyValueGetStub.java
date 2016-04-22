package com.sequenceiq.it.mock.restito.consul;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.glassfish.grizzly.http.Method;

import com.ecwid.consul.v1.kv.model.GetValue;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class ConsulKeyValueGetStub extends RestitoStub {

    public static final String PATH = "/kv/.*";
    public static final String FINISHED_VALUE = "FINISHED";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(CONSUL_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.contentType("application/json"), Action.stringContent(convertToJson(createValueList())));
    }

    private List<GetValue> createValueList() {
        List<GetValue> getValueList = new ArrayList<>();
        GetValue getValue = new GetValue();
        getValue.setValue(Base64.encodeBase64String(FINISHED_VALUE.getBytes()));
        getValueList.add(getValue);
        return getValueList;
    }
}

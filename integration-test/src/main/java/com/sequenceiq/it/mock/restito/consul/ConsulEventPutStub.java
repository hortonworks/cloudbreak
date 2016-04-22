package com.sequenceiq.it.mock.restito.consul;

import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.ecwid.consul.v1.event.model.Event;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class ConsulEventPutStub extends RestitoStub {

    public static final String PATH = "/event/fire/.*";

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.PUT), Condition.matchesUri(Pattern.compile(CONSUL_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.contentType("application/json"), Action.stringContent(convertToJson(createEvent())));
    }

    private Event createEvent() {
        Event event = new Event();
        event.setId("1");
        return event;
    }
}

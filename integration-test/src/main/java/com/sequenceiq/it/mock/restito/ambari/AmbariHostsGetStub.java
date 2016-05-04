package com.sequenceiq.it.mock.restito.ambari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.glassfish.grizzly.http.Method;

import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.model.Hosts;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;

public class AmbariHostsGetStub extends RestitoStub {

    public static final String PATH = "/hosts.*";
    private int serverNumber;

    public AmbariHostsGetStub(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    private Map<String, ?> getHostResponse() {

        List<Map<String, ?>> itemList = new ArrayList<>();
        for (int i = 1; i <= serverNumber; i++) {
            Hosts hosts = new Hosts(Collections.singletonList("host" + i), "HEALTHY");
            itemList.add(Collections.singletonMap("Hosts", hosts));
        }

        return Collections.singletonMap("items", itemList);
    }

    @Override
    public Condition getCondition() {
        return Condition.composite(Condition.method(Method.GET), Condition.matchesUri(Pattern.compile(AMBARI_API_ROOT + PATH)));
    }

    @Override
    public Action getAction() {
        return Action.composite(Action.ok(), Action.stringContent(convertToJson(getHostResponse())));
    }
}

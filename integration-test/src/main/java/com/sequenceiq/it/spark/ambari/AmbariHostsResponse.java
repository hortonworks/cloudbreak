package com.sequenceiq.it.spark.ambari;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.model.Hosts;

import spark.Request;
import spark.Response;

public class AmbariHostsResponse extends ITResponse {
    private int serverNumber;

    public AmbariHostsResponse(int serverNumber) {
        this.serverNumber = serverNumber;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        List<Map<String, ?>> itemList = new ArrayList<>();
        for (int i = 1; i <= serverNumber; i++) {
            Hosts hosts = new Hosts("host" + i, "HEALTHY");
            itemList.add(Collections.singletonMap("Hosts", hosts));
        }

        return Collections.singletonMap("items", itemList);
    }
}

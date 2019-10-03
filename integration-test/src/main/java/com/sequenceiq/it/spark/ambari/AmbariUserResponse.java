package com.sequenceiq.it.spark.ambari;

import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;

import java.util.Collections;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class AmbariUserResponse extends ITResponse {

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("text/plain");
        return gson().toJson(Collections.singletonMap("Users", "cloudbreak"));
    }

}

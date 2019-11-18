package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.TopologySuffix;

import spark.Request;
import spark.Response;

@Component
public class TopologySuffixFindResponse extends AbstractFreeIpaResponse<Set<TopologySuffix>> {
    @Override
    public String method() {
        return "topologysuffix_find";
    }

    @Override
    protected Set<TopologySuffix> handleInternal(Request request, Response response) {
        TopologySuffix suffix = new TopologySuffix();
        suffix.setCn("dummy");
        suffix.setDn("dummy");
        return Set.of(suffix);
    }
}

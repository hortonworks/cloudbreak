package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.TopologySuffix;

@Component
public class TopologySuffixFindResponse extends AbstractFreeIpaResponse<Set<TopologySuffix>> {
    @Override
    public String method() {
        return "topologysuffix_find";
    }

    @Override
    protected Set<TopologySuffix> handleInternal(String body) {
        TopologySuffix suffix = new TopologySuffix();
        suffix.setCn("dummy");
        suffix.setDn("dummy");
        return Set.of(suffix);
    }
}

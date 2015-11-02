package com.sequenceiq.cloudbreak.cloud.template.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class ContextBuilders {

    @Inject
    private List<ResourceContextBuilder> contextBuilders;
    private Map<Platform, ResourceContextBuilder> map = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ResourceContextBuilder builder : contextBuilders) {
            map.put(builder.platform(), builder);
        }
    }

    public ResourceContextBuilder get(Platform platform) {
        return map.get(platform);
    }

}

package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResponseProviders {
    @Inject
    private List<ResponseProvider> responseProviders;

    private final Map<String, ResponseProvider> map = new HashMap<>();

    @PostConstruct
    public void init() {
        responseProviders.forEach(rp -> map.put(rp.type(), rp));
    }

    public ResponseProvider get(String type) {
        return map.get(type);
    }
}

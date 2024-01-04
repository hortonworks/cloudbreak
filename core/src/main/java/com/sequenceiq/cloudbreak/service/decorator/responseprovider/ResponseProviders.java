package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

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

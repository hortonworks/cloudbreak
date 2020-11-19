package com.sequenceiq.mock.freeipa;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class FreeipaStoreService {

    private Map<String, FreeIpaDto> freeIpaDtos = new HashMap<>();

    public void terminate(String mockuuid) {
        freeIpaDtos.remove(mockuuid);
    }

    public Collection<FreeIpaDto> getAll() {
        return freeIpaDtos.values();
    }
}

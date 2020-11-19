package com.sequenceiq.mock.salt;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

@Component
public class SaltStoreService {

    private Map<String, SaltDto> saltDtos = new HashMap<>();

    public void setMinions(String mockUuid, List<Minion> minions) {
        SaltDto saltDto = saltDtos.computeIfAbsent(mockUuid, key -> new SaltDto(mockUuid));
        saltDto.setMinions(minions);
    }

    public List<Minion> getMinions(String mockUuid) {
        return read(mockUuid).getMinions();
    }

    public Map<String, Multimap<String, String>> getGrains(String mockUuid) {
        return read(mockUuid).getGrains();
    }

    public SaltDto read(String mockUuid) {
        SaltDto saltDto = saltDtos.get(mockUuid);
        if (saltDto == null) {
            throw new ResponseStatusException(NOT_FOUND, "SaltDto cannot be found by uuid: " + mockUuid);
        }
        return saltDto;
    }

    public void terminate(String mockuuid) {
        saltDtos.remove(mockuuid);
    }

    public Collection<SaltDto> getAll() {
        return saltDtos.values();
    }
}

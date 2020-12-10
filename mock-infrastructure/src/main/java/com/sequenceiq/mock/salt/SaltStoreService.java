package com.sequenceiq.mock.salt;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

@Component
public class SaltStoreService {

    private Map<String, SaltDto> saltDtos = new HashMap<>();

    public List<Minion> getMinions(String mockUuid) {
        return read(mockUuid).getSaltAction().getMinions();
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

    public void addRunResponse(String mockUuid, RunResponseDto runResponseDto) {
        SaltDto saltDto = read(mockUuid);
        List<RunResponseDto> runResponsDtos = saltDto.getRunResponses();
        runResponsDtos.add(runResponseDto);
    }

    public void saltbootFileDistribute(String mockUuid, MultipartFile body) {
        SaltDto saltDto = read(mockUuid);
        saltDto.getFileDistributonDtos().add(new FileDistributonDto(mockUuid, body.getOriginalFilename(), body.getSize(), body.getContentType()));
    }

    public void setSaltAction(String mockUuid, SaltAction saltAction) {
        read(mockUuid).setSaltAction(saltAction);
    }

    public void addPillar(String mockUuid, Pillar pillar) {
        read(mockUuid).getPillars().add(pillar);
    }

    public void create(String mockuuid) {
        saltDtos.put(mockuuid, new SaltDto(mockuuid));
    }
}

package com.sequenceiq.mock.salt;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

@Component
public class SaltStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStoreService.class);

    private Map<String, SaltDto> saltDtos = new ConcurrentHashMap<>();

    public List<Minion> getMinions(String mockUuid) {
        LOGGER.trace("read salt minions by {}", mockUuid);
        return read(mockUuid).getMinions();
    }

    public Map<String, Multimap<String, String>> getGrains(String mockUuid) {
        LOGGER.trace("read salt grains by {}", mockUuid);
        return read(mockUuid).getGrains();
    }

    public SaltDto read(String mockUuid) {
        LOGGER.trace("read salt by {}", mockUuid);
        SaltDto saltDto = saltDtos.get(mockUuid);
        if (saltDto == null) {
            LOGGER.info("cannot be found the salt by {}", mockUuid);
            throw new ResponseStatusException(NOT_FOUND, "SaltDto cannot be found by uuid: " + mockUuid);
        }
        return saltDto;
    }

    public void terminate(String mockUuid) {
        LOGGER.info("terminate salt by {}", mockUuid);
        saltDtos.remove(mockUuid);
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
        LOGGER.debug("add salt file distribute to {}", mockUuid);
        SaltDto saltDto = read(mockUuid);
        saltDto.getFileDistributonDtos().add(new FileDistributonDto(mockUuid, body.getOriginalFilename(), body.getSize(), body.getContentType()));
    }

    public void setSaltAction(String mockUuid, SaltAction saltAction) {
        LOGGER.debug("set salt actions to {}. Salt action: {}", mockUuid, saltAction);
        read(mockUuid).setSaltAction(saltAction);
        addMinions(mockUuid, saltAction.getMinions());
    }

    public void addMinions(String mockUuid, List<Minion> minions) {
        SaltDto saltDto = read(mockUuid);
        List<Minion> newMinions = new ArrayList<>();
        for (Minion minion : minions) {
            boolean exist = saltDto.getMinions().stream().anyMatch(m -> m.getAddress().equals(minion.getAddress()));
            if (!exist) {
                newMinions.add(minion);
            }
        }
        saltDto.getMinions().addAll(newMinions);
    }

    public void addPillar(String mockUuid, Pillar pillar) {
        LOGGER.debug("add salt pillar to {}. Pillar: {}", mockUuid, pillar);
        read(mockUuid).getPillars().add(pillar);
    }

    public void create(String mockUuid) {
        LOGGER.debug("create salt for {}", mockUuid);
        saltDtos.put(mockUuid, new SaltDto(mockUuid));
    }
}

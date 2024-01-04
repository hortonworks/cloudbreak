package com.sequenceiq.mock.salt.response;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.salt.SaltStoreService;

@Component
public class KeyDeleteSaltResponse implements SaltResponse {

    @Inject
    private SaltStoreService saltStoreService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<Minion> minions = saltStoreService.getMinions(mockUuid);
        minions.removeIf(minion -> params.get("match").contains(minion.getHostName() + "." + minion.getDomain()));
        return "";
    }

    @Override
    public String cmd() {
        return "key.delete";
    }
}

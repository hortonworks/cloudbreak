package com.sequenceiq.mock.salt.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.mock.salt.SaltResponse;

@Component
public class FileExistsSaltResponse implements SaltResponse {

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<String> targets = params.get("tgt");
        PingResponse pingResponse = new PingResponse();
        List<Map<String, Boolean>> result = new ArrayList<>();
        Map<String, Boolean> hostMap = Maps.newHashMap();
        targets.stream().forEach(target -> hostMap.put(target, false));
        result.add(hostMap);
        pingResponse.setResult(result);
        return pingResponse;
    }

    @Override
    public String cmd() {
        return "file.file_exists";
    }
}

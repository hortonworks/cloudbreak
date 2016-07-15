package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;

@Component
public class JsonToHDPRepoConverter extends AbstractConversionServiceAwareConverter<AmbariStackDetailsJson, HDPRepo>  {
    @Override
    public HDPRepo convert(AmbariStackDetailsJson source) {
        HDPRepo repo = new HDPRepo();
        Map<String, String> stack = new HashMap<>();
        Map<String, String> util = new HashMap<>();

        stack.put("repoid", source.getStackRepoId());
        stack.put(source.getOs(), source.getStackBaseURL());
        util.put("repoid", source.getUtilsRepoId());
        util.put(source.getOs(), source.getUtilsBaseURL());

        repo.setStack(stack);
        repo.setUtil(util);
        repo.setVerify(source.getVerify());
        repo.setHdpVersion(source.getVersion());
        return repo;
    }
}

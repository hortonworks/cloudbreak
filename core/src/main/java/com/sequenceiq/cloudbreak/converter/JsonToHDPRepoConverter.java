package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;

@Component
public class JsonToHDPRepoConverter extends AbstractConversionServiceAwareConverter<AmbariStackDetailsJson, HDPRepo> {

    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    @Override
    public HDPRepo convert(AmbariStackDetailsJson source) {
        HDPRepo repo = new HDPRepo();
        Map<String, String> stack = new HashMap<>();
        Map<String, String> util = new HashMap<>();

        stack.put("repoid", source.getStackRepoId());
        util.put("repoid", source.getUtilsRepoId());

        String stackBaseURL = source.getStackBaseURL();
        String utilsBaseURL = source.getUtilsBaseURL();
        if (source.getOs() == null) {
            stack.put(REDHAT_6, stackBaseURL);
            stack.put(REDHAT_7, stackBaseURL);
            util.put(REDHAT_6, utilsBaseURL);
            util.put(REDHAT_7, utilsBaseURL);
        } else {
            stack.put(source.getOs(), stackBaseURL);
            util.put(source.getOs(), utilsBaseURL);
        }

        repo.setStack(stack);
        repo.setUtil(util);
        repo.setVerify(source.getVerify());
        repo.setHdpVersion(source.getVersion());
        return repo;
    }
}

package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AmbariInfoMapperToAmbariInfoV4ResponseConverter extends AbstractConversionServiceAwareConverter<AmbariInfo, AmbariInfoV4Response> {
    @Override
    public AmbariInfoV4Response convert(AmbariInfo source) {
        AmbariInfoV4Response ambariInfoJson = new AmbariInfoV4Response();
        ambariInfoJson.setVersion(source.getVersion());
        ambariInfoJson.setRepository(stringAmbariRepoDetailsMapToStringAmbariRepoDetailsJsonMap(source.getRepo()));
        return ambariInfoJson;
    }

    private Map<String, AmbariRepositoryV4Response> stringAmbariRepoDetailsMapToStringAmbariRepoDetailsJsonMap(Map<String, AmbariRepoDetails> map) {
        if (map == null) {
            return null;
        }

        Map<String, AmbariRepositoryV4Response> ret = new HashMap<>();
        map.forEach((key, value) -> ret.put(key, getConversionService().convert(value, AmbariRepositoryV4Response.class)));
        return ret;
    }
}

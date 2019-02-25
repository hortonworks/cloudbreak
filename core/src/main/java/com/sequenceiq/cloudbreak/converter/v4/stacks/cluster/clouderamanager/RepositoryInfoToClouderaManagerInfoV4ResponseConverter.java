package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class RepositoryInfoToClouderaManagerInfoV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<RepositoryInfo, ClouderaManagerInfoV4Response> {

    @Override
    public ClouderaManagerInfoV4Response convert(RepositoryInfo source) {
        ClouderaManagerInfoV4Response cmInfoJson = new ClouderaManagerInfoV4Response();
        cmInfoJson.setVersion(source.getVersion());
        cmInfoJson.setRepository(stringCMRepoDetailsMapToStringCMRepoDetailsJsonMap(source.getRepo()));
        return cmInfoJson;
    }

    private Map<String, ClouderaManagerRepositoryV4Response> stringCMRepoDetailsMapToStringCMRepoDetailsJsonMap(Map<String, RepositoryDetails> map) {
        if (map == null) {
            return null;
        }

        Map<String, ClouderaManagerRepositoryV4Response> ret = new HashMap<>();
        map.forEach((key, value) -> ret.put(key, getConversionService().convert(value, ClouderaManagerRepositoryV4Response.class)));
        return ret;
    }
}

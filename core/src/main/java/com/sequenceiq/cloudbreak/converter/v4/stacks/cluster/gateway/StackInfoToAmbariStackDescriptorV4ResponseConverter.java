package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.AmbariStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackInfoToAmbariStackDescriptorV4ResponseConverter extends AbstractConversionServiceAwareConverter<StackInfo, AmbariStackDescriptorV4Response> {

    @Autowired
    private ConverterUtil converterUtil;

    @Override
    public AmbariStackDescriptorV4Response convert(StackInfo source) {
        AmbariStackDescriptorV4Response stackDescriptorV4 = new AmbariStackDescriptorV4Response();

        stackDescriptorV4.setVersion(source.getVersion());
        stackDescriptorV4.setMinAmbari(source.getMinAmbari());
        stackDescriptorV4.setRepository(defaultStackRepoDetailsToStackRepoDetailsV4Response(source.getRepo()));
        if (source.getRepo().getMpacks() != null) {
            Map<String, List<ManagementPackV4Entry>> map = new HashMap<>();
            source.getRepo().getMpacks().forEach((key, value) -> map.put(key, converterUtil.convertAll(value, ManagementPackV4Entry.class)));
            stackDescriptorV4.setMpacks(map);
        }
        return stackDescriptorV4;
    }

    private AmbariStackRepoDetailsV4Response defaultStackRepoDetailsToStackRepoDetailsV4Response(AmbariDefaultStackRepoDetails ambariDefaultStackRepoDetails) {
        if (ambariDefaultStackRepoDetails == null) {
            return null;
        }

        AmbariStackRepoDetailsV4Response ambariStackRepoDetailsV4Response = new AmbariStackRepoDetailsV4Response();
        ambariStackRepoDetailsV4Response.setStack(ambariDefaultStackRepoDetails.getStack());
        ambariStackRepoDetailsV4Response.setUtil(ambariDefaultStackRepoDetails.getUtil());
        return ambariStackRepoDetailsV4Response;
    }
}

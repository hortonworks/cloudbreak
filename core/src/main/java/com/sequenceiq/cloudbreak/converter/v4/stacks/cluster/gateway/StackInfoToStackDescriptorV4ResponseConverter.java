package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.StackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackInfoToStackDescriptorV4ResponseConverter extends AbstractConversionServiceAwareConverter<StackInfo, StackDescriptorV4Response> {

    @Autowired
    private ConverterUtil converterUtil;

    @Override
    public StackDescriptorV4Response convert(StackInfo source) {
        StackDescriptorV4Response stackDescriptorV4 = new StackDescriptorV4Response();

        stackDescriptorV4.setVersion(source.getVersion());
        stackDescriptorV4.setMinAmbari(source.getMinAmbari());
        stackDescriptorV4.setRepo(defaultStackRepoDetailsToStackRepoDetailsV4Response(source.getRepo()));
        if (source.getRepo().getMpacks() != null) {
            Map<String, List<ManagementPackV4Entry>> map = new HashMap<>();
            source.getRepo().getMpacks().forEach((key, value) -> {
                map.put(key, converterUtil.convertAll(value, ManagementPackV4Entry.class));
            });
            stackDescriptorV4.setMpacks(map);
        }
        return stackDescriptorV4;
    }

    private StackRepoDetailsV4Response defaultStackRepoDetailsToStackRepoDetailsV4Response(DefaultStackRepoDetails defaultStackRepoDetails) {
        if (defaultStackRepoDetails == null) {
            return null;
        }

        StackRepoDetailsV4Response stackRepoDetailsV4Response = new StackRepoDetailsV4Response();
        stackRepoDetailsV4Response.setStack(defaultStackRepoDetails.getStack());
        stackRepoDetailsV4Response.setUtil(defaultStackRepoDetails.getUtil());
        return stackRepoDetailsV4Response;
    }
}

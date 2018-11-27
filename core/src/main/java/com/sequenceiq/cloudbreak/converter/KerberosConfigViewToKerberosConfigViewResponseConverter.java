package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosViewResponse;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KerberosConfigViewToKerberosConfigViewResponseConverter extends AbstractConversionServiceAwareConverter<KerberosConfig,
        KerberosViewResponse> {

    @Override
    public KerberosViewResponse convert(KerberosConfig entity) {
        KerberosViewResponse response = new KerberosViewResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setType(entity.getType());
        response.setDescription(entity.getDescription());
        response.setEnvironments(entity.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        return response;
    }

}

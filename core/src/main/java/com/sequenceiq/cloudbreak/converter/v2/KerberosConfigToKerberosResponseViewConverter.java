package com.sequenceiq.cloudbreak.converter.v2;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KerberosConfigToKerberosResponseViewConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosViewV4Response> {

    @Override
    public KerberosViewV4Response convert(KerberosConfig source) {
        KerberosViewV4Response view = new KerberosViewV4Response();
        view.setName(source.getName());
        view.setType(source.getType());
        view.setId(source.getId());
        view.setEnvironments(source.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        view.setDescription(source.getDescription());
        return view;
    }

}

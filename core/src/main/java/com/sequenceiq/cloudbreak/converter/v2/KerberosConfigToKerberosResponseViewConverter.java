package com.sequenceiq.cloudbreak.converter.v2;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KerberosConfigToKerberosResponseViewConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosViewResponse> {

    @Override
    public KerberosViewResponse convert(KerberosConfig source) {
        KerberosViewResponse view = new KerberosViewResponse();
        view.setName(source.getName());
        view.setType(source.getType());
        view.setId(source.getId());
        view.setEnvironments(source.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        view.setDescription(source.getDescription());
        return view;
    }

}

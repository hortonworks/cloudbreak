package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public abstract class CompactViewToCompactViewResponseConverter<S extends CompactView, T extends CompactViewV4Response>
        extends AbstractConversionServiceAwareConverter<S, T> {
    @Override
    public T convert(S source) {
        T json = createTarget();
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setId(source.getId());
        return json;
    }

    protected abstract T createTarget();
}

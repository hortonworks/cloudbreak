package com.sequenceiq.provisioning.converter;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.provisioning.domain.ProvisionEntity;
import com.sequenceiq.provisioning.json.JsonEntity;

public abstract class AbstractConverter<J extends JsonEntity, E extends ProvisionEntity> implements Converter<J, E> {

    @Override
    public List<E> convertAllJsonToEntity(Collection<J> jsonList) {
        return FluentIterable.from(jsonList).transform(new Function<J, E>() {
            @Override
            public E apply(J j) {
                return convert(j);
            }
        }).toList();
    }

    @Override
    public List<J> convertAllEntityToJson(Collection<E> entityList) {
        return FluentIterable.from(entityList).transform(new Function<E, J>() {
            @Override
            public J apply(E e) {
                return convert(e);
            }
        }).toList();
    }
}

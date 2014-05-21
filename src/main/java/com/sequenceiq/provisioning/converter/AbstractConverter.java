package com.sequenceiq.provisioning.converter;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.provisioning.controller.json.JsonEntity;
import com.sequenceiq.provisioning.domain.ProvisionEntity;

public abstract class AbstractConverter<J extends JsonEntity, E extends ProvisionEntity> implements Converter<J, E> {

    @Override
    public Set<E> convertAllJsonToEntity(Collection<J> jsonList) {
        return FluentIterable.from(jsonList).transform(new Function<J, E>() {
            @Override
            public E apply(J j) {
                return convert(j);
            }
        }).toSet();
    }

    @Override
    public Set<J> convertAllEntityToJson(Collection<E> entityList) {
        return FluentIterable.from(entityList).transform(new Function<E, J>() {
            @Override
            public J apply(E e) {
                return convert(e);
            }
        }).toSet();
    }
}

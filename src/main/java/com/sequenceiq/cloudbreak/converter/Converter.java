package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;

import com.sequenceiq.cloudbreak.controller.json.JsonEntity;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

public interface Converter<J extends JsonEntity, E extends ProvisionEntity> {

    J convert(E entity);

    E convert(J json);

    Collection<E> convertAllJsonToEntity(Collection<J> jsonList);

    Collection<J> convertAllEntityToJson(Collection<E> entityList);

}


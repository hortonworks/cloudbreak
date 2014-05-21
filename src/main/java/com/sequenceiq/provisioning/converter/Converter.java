package com.sequenceiq.provisioning.converter;

import java.util.Collection;

import com.sequenceiq.provisioning.controller.json.JsonEntity;
import com.sequenceiq.provisioning.domain.ProvisionEntity;

public interface Converter<J extends JsonEntity, E extends ProvisionEntity> {

    J convert(E entity);

    E convert(J json);

    Collection<E> convertAllJsonToEntity(Collection<J> jsonList);

    Collection<J> convertAllEntityToJson(Collection<E> entityList);

}


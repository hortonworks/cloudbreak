package com.sequenceiq.provisioning.converter;

import java.util.Collection;

import com.sequenceiq.provisioning.domain.ProvisionEntity;
import com.sequenceiq.provisioning.json.JsonEntity;

public interface Converter<J extends JsonEntity, E extends ProvisionEntity> {

    J convert(E entity);

    E convert(J json);

    Collection<E> convertAllJsonToEntity(Collection<J> jsonList);

    Collection<J> convertAllEntityToJson(Collection<E> entityList);

}


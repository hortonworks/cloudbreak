package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.controller.json.JsonEntity;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public abstract class AbstractConverter<J extends JsonEntity, E extends ProvisionEntity> implements Converter<J, E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConverter.class);

    @Override
    public Set<E> convertAllJsonToEntity(Collection<J> jsonList) {
        Set<E> result = new HashSet<>();
        for (J j : jsonList) {
            try {
                result.add(convert(j));
            } catch (Exception ex) {
                MDCBuilder.buildMdcContext(j);
                LOGGER.error("Can not convert json object to entity");
            }
        }
        return result;
    }

    @Override
    public Set<J> convertAllEntityToJson(Collection<E> entityList) {
        Set<J> result = new HashSet<>();
        for (E e : entityList) {
            try {
                result.add(convert(e));
            } catch (Exception ex) {
                MDCBuilder.buildMdcContext(e);
                LOGGER.error("Can not convert entity object to json");
            }
        }
        return result;
    }
}

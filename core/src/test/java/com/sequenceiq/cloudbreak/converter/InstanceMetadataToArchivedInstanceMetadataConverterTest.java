package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openpojo.random.RandomFactory;
import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoField;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.utils.ValidationHelper;
import com.sequenceiq.cloudbreak.domain.stack.instance.ArchivedInstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

class InstanceMetadataToArchivedInstanceMetadataConverterTest {

    @Test
    public void convert() throws Exception {
        InstanceMetadataToArchivedInstanceMetadataConverter underTest = new InstanceMetadataToArchivedInstanceMetadataConverter();
        InstanceMetaData instanceMetaData = generateInstanceMetaData();
        ArchivedInstanceMetaData result = underTest.convert(instanceMetaData);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        String imJson = objectMapper.writeValueAsString(instanceMetaData);
        String aimJson = objectMapper.writeValueAsString(result);
        assertEquals(objectMapper.readTree(imJson), objectMapper.readTree(aimJson),
                "Converted ArchivedInstanceMetaData should have the same fields and values.");
    }

    private InstanceMetaData generateInstanceMetaData() {
        PojoClass pojoClass = PojoClassFactory.getPojoClass(InstanceMetaData.class);
        final Object classInstance = ValidationHelper.getBasicInstance(pojoClass);
        for (final PojoField fieldEntry : pojoClass.getPojoFields()) {
            if (fieldEntry.hasGetter()) {
                Object value = fieldEntry.get(classInstance);

                if (!fieldEntry.isFinal()) {
                    value = RandomFactory.getRandomValue(fieldEntry);
                    fieldEntry.set(classInstance, value);
                }

            }
        }

        InstanceMetaData instanceMetaData = (InstanceMetaData) classInstance;
        instanceMetaData.setServer(instanceMetaData.getClusterManagerServer());
        return instanceMetaData;
    }
}
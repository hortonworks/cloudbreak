package com.sequenceiq.cloudbreak.domain.json

import javax.persistence.AttributeConverter

class JsonToString : AttributeConverter<Json, String> {
    override fun convertToDatabaseColumn(attribute: Json?): String? {
        if (attribute != null) {
            return attribute.value
        }
        return null
    }

    override fun convertToEntityAttribute(dbData: String): Json {
        return Json(dbData)
    }
}

package com.sequenceiq.redbeams.converter;

import javax.persistence.AttributeConverter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Component
public class CrnConverter implements AttributeConverter<Crn, String> {

    /**
     * Converts a CRN into a string for use in a database entity.
     *
     * @param  attribute CRN
     * @return string CRN
     */
    @Override
    public String convertToDatabaseColumn(Crn attribute) {
        return attribute.toString();
    }

    /**
     * Converts a string CRN from a database entity into a Crn object. Fails
     * if the CRN string cannot be parsed.
     *
     * @param  dbData CRN string
     * @return parsed CRN
     * @throws NullPointerException if the CRN string is null
     * @throws CrnParseException if the CRN cannot be parsed
     */
    @Override
    public Crn convertToEntityAttribute(String dbData) {
        return Crn.safeFromString(dbData);
    }
}

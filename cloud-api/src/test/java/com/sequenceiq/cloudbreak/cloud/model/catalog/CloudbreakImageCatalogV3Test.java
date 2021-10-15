package com.sequenceiq.cloudbreak.cloud.model.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class CloudbreakImageCatalogV3Test {

    @Test
    public void shouldBeTheSameAfterDeserializationAndSerialization() throws IOException {
        String imageCatalogFromFile = FileReaderUtils.readFileFromClasspath("image-catalog.json");

        assertNotNull(imageCatalogFromFile);
        assertNotEquals("", imageCatalogFromFile);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();

        CloudbreakImageCatalogV3 imageCatalog = objectMapper.readValue(imageCatalogFromFile, CloudbreakImageCatalogV3.class);
        String imageCatalogAfterSerialization = objectWriter.writeValueAsString(imageCatalog);

        CloudbreakImageCatalogV3 deserializedImageCatalog = objectMapper.readValue(imageCatalogAfterSerialization, CloudbreakImageCatalogV3.class);
        String imageCatalogAfterSecondSerialization = objectWriter.writeValueAsString(deserializedImageCatalog);

        assertEquals(imageCatalogAfterSerialization, imageCatalogAfterSecondSerialization);
    }
}
package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class ImageCatalogTest {

    @Test
    void shouldSerializeAndDeserializeTheSame() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        String catalogString = FileReaderUtils.readFileFromClasspath("image-catalog.json");

        Assertions.assertNotNull(catalogString);
        Assertions.assertNotEquals("", catalogString);

        ImageCatalog imageCatalog = objectMapper.readValue(catalogString, ImageCatalog.class);
        String firstWrite = objectWriter.writeValueAsString(imageCatalog);
        ImageCatalog imageCatalogFromJson = objectMapper.readValue(firstWrite, ImageCatalog.class);
        String secondWrite = objectWriter.writeValueAsString(imageCatalogFromJson);

        Assertions.assertEquals(firstWrite, secondWrite);
    }

}
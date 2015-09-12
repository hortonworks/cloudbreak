package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.HashMap;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.FileSystemRequest;
import com.sequenceiq.cloudbreak.domain.FileSystem;

@Component
public class JsonToFileSystemConverter extends AbstractConversionServiceAwareConverter<FileSystemRequest, FileSystem> {
    @Override
    public FileSystem convert(FileSystemRequest source) {
        FileSystem fs = new FileSystem();
        fs.setName(source.getName());
        fs.setType(source.getType().name());
        fs.setDefaultFs(source.isDefaultFs());
        if (source.getProperties() != null) {
            fs.setProperties(source.getProperties());
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(fs.getProperties());
                mapper.readValue(json, source.getType().getClazz());
            } catch (IOException e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        } else {
            fs.setProperties(new HashMap<String, String>());
        }
        return fs;
    }
}

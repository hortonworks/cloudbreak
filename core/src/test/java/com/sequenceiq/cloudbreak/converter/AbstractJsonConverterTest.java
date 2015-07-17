package com.sequenceiq.cloudbreak.converter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.TestException;

public abstract class AbstractJsonConverterTest<S> extends AbstractConverterTest {

    private List<String> defaultSkippedFields = Arrays.asList("id", "owner", "account");

    public S getRequest(String jsonFilePath) {
        return readJsonFile(jsonFilePath, getRequestClass());
    }

    public abstract Class<S> getRequestClass();

    @Override
    public void assertAllFieldsNotNull(Object obj) {
        super.assertAllFieldsNotNull(obj, defaultSkippedFields);
    }

    @Override
    public void assertAllFieldsNotNull(Object object, List<String> skippedFields) {
        List<String> newFields = new ArrayList<>();
        newFields.addAll(defaultSkippedFields);
        newFields.addAll(skippedFields);
        super.assertAllFieldsNotNull(object, newFields);
    }

    private S readJsonFile(String jsonPath, Class<S> clazz) {
        try {
            String classPackage = getClass().getPackage().getName().replaceAll("\\.", "/");
            Resource resource = new ClassPathResource(classPackage + "/" + jsonPath);
            ObjectMapper mapper = new ObjectMapper();
            BufferedReader fileReader = new BufferedReader(
                    new FileReader(resource.getFile()));
            return mapper.readValue(fileReader, clazz);
        } catch (IOException e) {
            throw new TestException(e);
        }
    }
}

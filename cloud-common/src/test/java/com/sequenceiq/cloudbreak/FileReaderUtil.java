package com.sequenceiq.cloudbreak;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class FileReaderUtil {

    private FileReaderUtil() {
    }

    public static String readResourceFile(Object caller, String fileName) {
        try {
            String classPackage = caller.getClass().getPackage().getName().replaceAll("\\.", "/");
            Resource resource = new ClassPathResource(classPackage + '/' + fileName);
            return IOUtils.toString(new FileInputStream(resource.getFile()), Charset.defaultCharset());
        } catch (IOException e) {
            throw new TestException(e);
        }
    }

}

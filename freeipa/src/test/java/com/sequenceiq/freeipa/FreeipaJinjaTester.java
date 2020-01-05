package com.sequenceiq.freeipa;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateError;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class FreeipaJinjaTester {

    @Test
    public void verifyFreeipaSaltFilesFiles() throws IOException {
        Collection<File> files = collectAllSlsFiles("freeipa-salt");
        assertTrue("At least one sls file should be verified", files.size() > 0);
        for (File slsFile : files) {
            verifySingleSaltFile(slsFile.toPath());
        }
    }

    private Collection<File> collectAllSlsFiles(String salt) throws IOException {
        File file = new ClassPathResource(salt).getFile();
        return FileUtils.listFiles(
                file,
                new RegexFileFilter("^(.*.sls)"),
                DirectoryFileFilter.DIRECTORY
        );
    }

    private void verifySingleSaltFile(Path path) throws IOException {
        String file = FileReaderUtils.readFileFromPath(path);
        Jinjava jinjava = new Jinjava();
        JinjavaInterpreter interpreter = jinjava.newInterpreter();
        interpreter.parse(file);
        List<TemplateError> errors = interpreter.getErrors();
        if (!errors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Jinja validation failed for sls file: " + path);
            errorMsg.append("\n");
            errors.forEach(error -> errorMsg.append(error));
            fail(errorMsg.toString());
        }
    }

}

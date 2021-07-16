package com.sequenceiq.cloudbreak.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class CompressUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressUtil.class);

    private CompressUtil() {
    }

    public byte[] generateCompressedOutputFromFolders(String... classpathFolders) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zout = new ZipOutputStream(baos)) {
                Map<String, List<Resource>> structure = new TreeMap<>();
                for (String classpathFolder : classpathFolders) {
                    fillStructureWithResources(structure, classpathFolder);
                }
                for (Map.Entry<String, List<Resource>> entry : structure.entrySet()) {
                    zout.putNextEntry(new ZipEntry(entry.getKey()));
                    for (Resource resource : entry.getValue()) {
                        LOGGER.debug("Zip entry: {}", resource.getFilename());
                        zout.putNextEntry(new ZipEntry(entry.getKey() + resource.getFilename()));
                        InputStream inputStream = resource.getInputStream();
                        byte[] bytes = IOUtils.toByteArray(inputStream);
                        zout.write(bytes);
                        zout.closeEntry();
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to zip file resources", e);
                throw new IOException("Failed to zip file resources", e);
            }
            return baos.toByteArray();
        }
    }

    private void fillStructureWithResources(Map<String, List<Resource>> structure,
            String folder) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource resource : resolver.getResources(String.format("classpath*:%s/**", folder))) {
            String path = resource.getURL().getPath();
            String folderWithSlash = String.format("/%s", folder);
            String dir = path.substring(path.indexOf(folderWithSlash) + folderWithSlash.length(), path.lastIndexOf('/') + 1);
            List<Resource> list = structure.getOrDefault(dir, new ArrayList<>());
            structure.put(dir, list);
            if (!path.endsWith("/")) {
                list.add(resource);
            }
        }
    }
}

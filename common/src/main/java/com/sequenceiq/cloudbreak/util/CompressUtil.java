package com.sequenceiq.cloudbreak.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class CompressUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressUtil.class);

    private static final int BYTES_BUFFER_SIZE = 1024;

    /**
     * It generates new compressed content in bytes from folders (in the classpath).
     * Generate compressed output
     * @param classpathFolders base folders that contain files that will be compressed
     * @return newly created compressed data in bytes
     */
    public byte[] generateCompressedOutputFromFolders(String... classpathFolders) throws IOException {
        return generateCompressedOutputFromFolders(Arrays.asList(classpathFolders), new ArrayList<>());
    }

    /**
     * It generates new compressed content in bytes from folders (in the classpath).
     * @param classpathFolders base folders that contain files that will be compressed.
     * @param prefixList compressed file will contain only those entries that are started with these prefix filters.
     * @return newly created compressed data in bytes
     */
    public byte[] generateCompressedOutputFromFolders(List<String> classpathFolders, List<String> prefixList) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zout = new ZipOutputStream(baos)) {
                writeMatchingComponentsToZipStreamFromResources(classpathFolders, prefixList, zout);
            } catch (IOException e) {
                LOGGER.error("Failed to zip file resources", e);
                throw new IOException("Failed to zip file resources", e);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Override existing compressed content, but only those files will be overwritten that are listed in the component filter.
     * @param classpathFolders base folders that contain files that will be compressed.
     * @param prefixList compressed file will contain only those entries that are started with these prefix filters.
     * @param bytesContent content of the original state/pillar (compressed)
     * @return newly created compressed data in bytes
     */
    public byte[] updateCompressedOutputFolders(List<String> classpathFolders, List<String> prefixList, byte[] bytesContent) throws IOException {
        LOGGER.debug("Overrides existing compressed content from the classpath with prefix filters: {}", prefixList);
        Optional.ofNullable(bytesContent).orElseThrow(() -> new IllegalArgumentException("compressed byte content cannot be empty."));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zout = new ZipOutputStream(bos)) {
                writeNonMatchingComponentsToZipStreamFromBytes(bytesContent, zout, prefixList);
                writeMatchingComponentsToZipStreamFromResources(classpathFolders, prefixList, zout);
            }
            return bos.toByteArray();
        }
    }

    /**
     * Compares contents of 2 compressed files.
     * @param c1 first compressed input to compare
     * @param c2 compressed input to compare
     * @param prefixList list of prefixes that are used as filters during collection the compressed entries.
     * @return returns true if the keys/values
     */
    public boolean compareCompressedContent(byte[] c1, byte[] c2, List<String> prefixList) throws IOException {
        Map<String, String> zipMap1 = getZipEntries(c1, prefixList);
        Map<String, String> zipMap2 =  getZipEntries(c2, prefixList);
        return zipMap1.equals(zipMap2);
    }

    public boolean compareCompressedContent(byte[] c1, byte[] c2) throws IOException {
        return compareCompressedContent(c1, c2, new ArrayList<>());
    }

    /**
     * Convert converted input (filenames + file contents) into a hash map.
     * @param bytesContent compressed input.
     * @param prefixList list of prefixes that are used as filters during collection the compressed entries.
     * @return key/value map that contain filename/content of the compressed data
     */
    public Map<String, String> getZipEntries(byte[] bytesContent, List<String> prefixList) throws IOException {
        Optional.ofNullable(bytesContent).orElseThrow(() -> new IllegalArgumentException("compressed byte content cannot be empty."));
        Map<String, String> result = new HashMap<>();
        String[] prefixes = prefixList.toArray(new String[0]);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytesContent)) {
            try (ZipInputStream zis = new ZipInputStream(bis)) {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        String name = ze.getName();
                        if (CollectionUtils.isNotEmpty(prefixList) && !StringUtils.startsWithAny(name, prefixes)) {
                            continue;
                        }
                        try (ByteArrayOutputStream bosForStr = new ByteArrayOutputStream()) {
                            String content = readBytesAsStringFromZip(zis, bosForStr);
                            result.put(name, content);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Map<String, String> getZipEntries(byte[] bytesContent) throws IOException {
        return getZipEntries(bytesContent, new ArrayList<>());
    }

    private void fillStructureWithResources(Map<String, List<Resource>> structure,
            String folder, List<String> prefixList) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (Resource resource : resolver.getResources(String.format("classpath*:%s/**", folder))) {
            String path = resource.getURL().getPath();
            String folderWithSlash = String.format("/%s", folder);
            String dir = path.substring(path.indexOf(folderWithSlash) + folderWithSlash.length(), path.lastIndexOf('/') + 1);
            if (isComponentIncludedInFilter(dir, prefixList)) {
                List<Resource> list = structure.getOrDefault(dir, new ArrayList<>());
                structure.put(dir, list);
                if (!path.endsWith("/")) {
                    list.add(resource);
                }
            }
        }
    }

    private void writeMatchingComponentsToZipStreamFromResources(List<String> classpathFolders, List<String> prefixList, ZipOutputStream zout)
            throws IOException {
        Map<String, List<Resource>> structure = new TreeMap<>();
        for (String classpathFolder : classpathFolders) {
            fillStructureWithResources(structure, classpathFolder, prefixList);
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
    }

    private void writeNonMatchingComponentsToZipStreamFromBytes(byte[] bytesContent, ZipOutputStream zos, List<String> prefixList)
            throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytesContent)) {
            try (ZipInputStream zis = new ZipInputStream(bis)) {
                writeNotMatchingComponentsToZipOutputStream(zos, zis, prefixList);
            }
        }
    }

    private void writeNotMatchingComponentsToZipOutputStream(ZipOutputStream zos, ZipInputStream zis, List<String> prefixList)
            throws IOException {
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            String name = ze.getName();
            if (!ze.isDirectory() && !isComponentIncludedInFilter(name, prefixList)) {
                try (ByteArrayOutputStream bosForStr = new ByteArrayOutputStream()) {
                    String content = readBytesAsStringFromZip(zis, bosForStr);
                    zos.putNextEntry(new ZipEntry(name));
                    zos.write(content.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
        }
    }

    String readBytesAsStringFromZip(ZipInputStream zis, ByteArrayOutputStream bosForStr) throws IOException {
        int n;
        byte[] buf = new byte[BYTES_BUFFER_SIZE];
        while ((n = zis.read(buf, 0, BYTES_BUFFER_SIZE)) > -1) {
            bosForStr.write(buf, 0, n);
        }
        return bosForStr.toString();
    }

    private boolean isComponentIncludedInFilter(String dir, List<String> prefixList) {
        return CollectionUtils.isEmpty(prefixList)
                || prefixList.stream().anyMatch(dir::startsWith);
    }
}

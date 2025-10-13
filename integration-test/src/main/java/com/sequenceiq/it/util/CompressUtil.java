package com.sequenceiq.it.util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class CompressUtil {

    private CompressUtil() {
    }

    @SuppressWarnings("checkstyle:RegexpSingleline")
    public static void compressDirectoryToTarGz(Path dirPath) throws IOException {
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Not a directory: " + dirPath);
        }

        String targetFileName = dirPath.getFileName().toString() + ".tar.gz";
        Path targetPath = dirPath.resolveSibling(targetFileName);

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            Files.walk(dirPath).forEach(path -> {
                if (path.equals(dirPath)) {
                    return;
                }
                String name = dirPath.relativize(path).toString();
                TarArchiveEntry entry = new TarArchiveEntry(path.toFile(), Files.isDirectory(path) ? name + "/" : name);

                try {
                    taos.putArchiveEntry(entry);
                    if (Files.isRegularFile(path)) {
                        Files.copy(path, taos);
                    }
                    taos.closeArchiveEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            taos.finish();
        }
    }
}

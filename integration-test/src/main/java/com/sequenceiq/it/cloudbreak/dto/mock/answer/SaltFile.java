package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

import com.sequenceiq.it.cloudbreak.exception.StoreFileException;

public class SaltFile {
    private String path;

    private String target;

    private File file;

    private SaltFile() {
    }

    public static SaltFile create(HttpServletRequest request) {
        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            Path tempDir = Files.createTempDirectory("temp");
            factory.setRepository(tempDir.toFile());
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items = upload.parseRequest(new ServletRequestContext(request));
            SaltFile saltFile = new SaltFile();
            for (FileItem item : items) {
                if (item.isFormField()) {
                    if ("path".equals(item.getFieldName())) {
                        saltFile.setPath(item.getString());
                    } else {
                        saltFile.setTarget(item.getString());
                    }
                } else {
                    File uploadedFile = new File(String.format("%s/saltfile%s.tmp", tempDir, UUID.randomUUID()));
                    item.write(uploadedFile);
                    saltFile.setFile(uploadedFile);
                }
            }

            return saltFile;
        } catch (Exception e) {
            throw new StoreFileException("Could not create temp file", e);
        }
    }

    public String getPath() {
        return path;
    }

    public String getTarget() {
        return target;
    }

    public File getFile() {
        return file;
    }

    private void setPath(String path) {
        this.path = path;
    }

    private void setTarget(String target) {
        this.target = target;
    }

    private void setFile(File file) {
        this.file = file;
    }
}

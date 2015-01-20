package com.sequenceiq.it.util;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

public class ResourceUtil {
    private ResourceUtil() {
    }

    public static String readStringFromResource(ApplicationContext applicationContext, String resourceLocation) throws IOException {
        return new String(readResource(applicationContext, resourceLocation));
    }

    public static String readBase64EncodedContentFromResource(ApplicationContext applicationContext, String resourceLocation) throws IOException {
        return Base64.encodeBase64String(readResource(applicationContext, resourceLocation));
    }

    public static byte[] readResource(ApplicationContext applicationContext, String resourceLocation) throws IOException {
        Resource resource = applicationContext.getResource(resourceLocation);
        return StreamUtils.copyToByteArray(resource.getInputStream());
    }
}

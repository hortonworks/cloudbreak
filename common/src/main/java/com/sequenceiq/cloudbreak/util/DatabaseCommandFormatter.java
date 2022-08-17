package com.sequenceiq.cloudbreak.util;

import org.springframework.stereotype.Component;

@Component
public class DatabaseCommandFormatter {

    public String encapsulateContentForLikelinessQuery(String content) {
        return "%:" + content + ":%";
    }

}

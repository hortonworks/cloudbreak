package com.sequenceiq.cloudbreak.jvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class NativeMemoryParser {

    private static final String RESERVED_PREFIX = "reserved=";

    private static final String COMMITTED_PREFIX = "committed=";

    private static final String KB_POSTFIX = "KB";

    private static final String CATEGORY_LINE_PREFIX = "-";

    private static final String CATEGORY_NAME_POSTFIX = "(";

    public List<MemoryCategory> parseVmNativeMemory(String vmNativeMemory) {
        List<MemoryCategory> vmNativeMemoryCategories = new ArrayList<>();
        Iterator<String> lines = Arrays.asList(vmNativeMemory.split("\n")).iterator();
        while (lines.hasNext()) {
            String line = lines.next();
            if (line.contains("Total:")) {
                String reserved = StringUtils.substringBetween(line, RESERVED_PREFIX, KB_POSTFIX);
                String committed = StringUtils.substringBetween(line, COMMITTED_PREFIX, KB_POSTFIX);
                vmNativeMemoryCategories.add(new MemoryCategory("Total", Double.valueOf(reserved), Double.valueOf(committed)));
            } else if (line.startsWith(CATEGORY_LINE_PREFIX)) {
                String name = StringUtils.substringBetween(line, CATEGORY_LINE_PREFIX, CATEGORY_NAME_POSTFIX).trim();
                String reserved = StringUtils.substringBetween(line, RESERVED_PREFIX, KB_POSTFIX);
                String committed = StringUtils.substringBetween(line, COMMITTED_PREFIX, KB_POSTFIX);
                vmNativeMemoryCategories.add(new MemoryCategory(name, Double.valueOf(reserved), Double.valueOf(committed)));
            }
        }
        return vmNativeMemoryCategories;
    }
}

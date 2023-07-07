package com.sequenceiq.cloudbreak.jvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class VmFlagsParser {

    public List<String> parseVmFlags(String vmFlags) {
        List<String> vmFlagList = new ArrayList<>();
        Iterator<String> lines = Arrays.asList(vmFlags.split("\n")).iterator();
        while (lines.hasNext()) {
            String line = lines.next();
            vmFlagList.add(StringUtils.deleteWhitespace(StringUtils.substringBefore(line, "{")));
        }
        return vmFlagList;
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.inifile;

import org.springframework.stereotype.Component;

@Component
public class IniFileFactory {

    public IniFile create() {
        return new IniFile();
    }

}

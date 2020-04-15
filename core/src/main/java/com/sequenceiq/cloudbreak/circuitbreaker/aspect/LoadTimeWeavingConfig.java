package com.sequenceiq.cloudbreak.circuitbreaker.aspect;

import static org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving.ENABLED;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.context.annotation.LoadTimeWeavingConfigurer;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver;

@Configuration
@EnableLoadTimeWeaving(aspectjWeaving = ENABLED)
public class LoadTimeWeavingConfig implements LoadTimeWeavingConfigurer {

    @Override
    public LoadTimeWeaver getLoadTimeWeaver() {
        return new TomcatLoadTimeWeaver();
        //return new ReflectiveLoadTimeWeaver();
    }

//    @Bean
//    public LoadTimeWeaver loadTimeWeaver() {
//        // this should enable load-time weaving in Tomcat
//        return new TomcatLoadTimeWeaver(new WebappClassLoader());
//    }

//    @Bean
//    public InstrumentationLoadTimeWeaver loadTimeWeaver() {
//        return new InstrumentationLoadTimeWeaver();
//    }

}

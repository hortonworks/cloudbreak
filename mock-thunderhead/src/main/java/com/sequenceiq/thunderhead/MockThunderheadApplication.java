package com.sequenceiq.thunderhead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"com.sequenceiq.cloudbreak.auth.crn", "com.sequenceiq.thunderhead", "com.sequenceiq.cloudbreak.dns"},
        exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class MockThunderheadApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            SpringApplication.run(MockThunderheadApplication.class);
        } else {
            SpringApplication.run(MockThunderheadApplication.class, args);
        }
    }
}
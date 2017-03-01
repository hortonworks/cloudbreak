package com.sequenceiq.cloudbreak.init;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;

@Component
public class DatabaseEncryptorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEncryptorService.class);

    @Inject
    private javax.sql.DataSource dataSource;

    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @PostConstruct
    public void init() throws SQLException {
        doEncryption("blueprint", new String[]{ "blueprinttext" }, () -> blueprintRepository.findFirstByIdGreaterThan(0L));
        String[] clusterFields = new String[]{ "username", "password", "blueprintcustomproperties" };
        doEncryption("cluster", clusterFields, () -> clusterRepository.findFirstByIdGreaterThan(0L));
    }

    private void doEncryption(String database, String[] fields, Supplier<?> check) throws SQLException {
        try {
            check.get();
        } catch (EncryptionOperationNotPossibleException e) {
            LOGGER.info("Encrypting " + database);
            try (Statement select = dataSource.getConnection().createStatement(); Statement update = dataSource.getConnection().createStatement()) {
                Joiner joiner = Joiner.on(",");
                select.execute(String.format("SELECT id, %s FROM %s", joiner.join(fields), database));
                try (ResultSet rs = select.getResultSet()) {
                    while (rs.next()) {
                        StringBuilder sb = new StringBuilder(String.format("UPDATE %s SET ", database));
                        sb.append(joiner.join(Arrays.stream(fields)
                                .map(f -> {
                                    try {
                                        return String.format("%s = '%s' ", f, encryptor.encrypt(rs.getString(f)));
                                    } catch (SQLException se) {
                                        throw new RuntimeException(se);
                                    }
                                }).collect(Collectors.toList())));
                        update.executeUpdate(sb.append("WHERE id = ").append(rs.getLong("id")).toString());
                    }
                }
            }
        }
    }
}

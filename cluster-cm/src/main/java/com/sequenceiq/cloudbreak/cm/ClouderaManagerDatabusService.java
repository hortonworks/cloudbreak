package com.sequenceiq.cloudbreak.cm;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class ClouderaManagerDatabusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDatabusService.class);

    private static final String ALTUS_ACCESS_KEY_ID = "altus_access_key_id";

    private static final String ALTUS_PRIVATE_KEY = "altus_private_key";

    private static final String DATABUS_CRN_PATTERN = "dataeng-wa-publisher-%s-%s";

    private static final String DATABUS_UPLOADER_RESOURCE_NAME = "DbusUploader";

    @Inject
    private GrpcUmsClient umsClient;

    @Value("${altus.databus.credential.file:}")
    private String altusDatabusCredentialFile;

    @Value("${altus.databus.credential.profile:default}")
    private String altusDatabusCredentialProfile;

    /**
     * Generate new machine user (if it is needed) and access api key for this user.
     * Also assign built-in dabaus uploader role for the machine user.
     * @param stack stack that is used to get user crn
     * @return cre
     */
    AltusCredential createMachineUserAndGenerateKeys(Stack stack) {
        String userCrn = stack.getCreator().getUserCrn();
        String machineUserName = getWAMachineUserName(userCrn, stack.getCluster().getId().toString());
        UserManagementProto.MachineUser machineUser = umsClient.createMachineUser(machineUserName, userCrn, Optional.empty());
        String builtInDbusRoleCrn = getBuiltInDatabusCrn();
        umsClient.assignMachineUserRole(userCrn, machineUser.getCrn(), builtInDbusRoleCrn, Optional.empty());
        return umsClient.generateAccessSecretKeyPair(userCrn, machineUser.getCrn(), Optional.empty());
    }

    /**
     * Cleanup machine user related resources (access keys, role, user)
     * @param stack stack that is used to get user crn
     */
    void cleanUpMachineUser(Stack stack) {
        if (isAltusDatabusCredentialNotFilled()) {
            try {
                String userCrn = stack.getCreator().getUserCrn();
                String machineUserName = getWAMachineUserName(userCrn, stack.getCluster().getId().toString());
                String builtInDbusRoleCrn = getBuiltInDatabusCrn();
                umsClient.unassignMachineUserRole(userCrn, machineUserName, builtInDbusRoleCrn, Optional.empty());
                umsClient.deleteMachineUserAccessKeys(userCrn, machineUserName, Optional.empty());
                umsClient.deleteMachineUser(machineUserName, userCrn, Optional.empty());
            } catch (Exception e) {
                LOGGER.warn("Cluster Databus resource cleanup failed. It is not a fatal issue, "
                        + "but note that you could have remaining UMS resources for your account", e);
            }
        } else {
            LOGGER.info("Skipping machine user deletion as api keys were provided manually.");
        }
    }

    @VisibleForTesting
    AltusCredential getAltusCredential(Stack stack) {
        String accessKey = "";
        String privateKey = "";
        if (isAltusDatabusCredentialNotFilled()) {
            AltusCredential credential = createMachineUserAndGenerateKeys(stack);
            accessKey = credential.getAccessKey();
            privateKey = trimAndReplacePrivateKey(credential.getPrivateKey());
        } else {
            LOGGER.warn("Altus access / private key pair is set directly through a file: {} (with profile: {})",
                    altusDatabusCredentialFile, altusDatabusCredentialProfile);
            try {
                FileReader fileReader = new FileReader(altusDatabusCredentialFile);
                Map<String, Properties> credPropsMap = parseINI(fileReader);
                Properties credPros = credPropsMap.get(altusDatabusCredentialProfile);
                accessKey = credPros.getProperty(ALTUS_ACCESS_KEY_ID);
                privateKey = trimAndReplacePrivateKey(credPros.getProperty(ALTUS_PRIVATE_KEY).toCharArray());
            } catch (IOException e) {
                LOGGER.error("Exception during reading altus credential file:", e);
                throw new RuntimeException(e);
            }
        }
        return new AltusCredential(accessKey, privateKey.toCharArray());
    }

    // Partition and region is hard coded right now,
    // if it will change use the same as the user crn
    @VisibleForTesting
    String getBuiltInDatabusCrn() {
        Crn databusCrn = Crn.builder()
                .setAccountId("altus")
                .setService(Crn.Service.IAM)
                .setResourceType(Crn.ResourceType.ROLE)
                .setResource(DATABUS_UPLOADER_RESOURCE_NAME)
                .build();
        return databusCrn.toString();
    }

    // CM expects the private key to come in as a single line, so we need to
    // encode the newlines. We trim it to avoid a CM warning that the key
    // should not start or end with whitespace.
    @VisibleForTesting
    String trimAndReplacePrivateKey(char[] privateKey) {
        return new String(privateKey).trim().replace("\n", "\\n");
    }

    @VisibleForTesting
    Map<String, Properties> parseINI(Reader reader) throws IOException {
        Map<String, Properties> result = new HashMap<>();
        new Properties() {

            private Properties section;

            @Override
            public Object put(Object key, Object value) {
                String header = String.format("%s %s", key, value).trim();
                if (header.startsWith("[") && header.endsWith("]")) {
                    section = new Properties();
                    return result.put(header.substring(1, header.length() - 1), section);
                } else {
                    return section.put(key, value);
                }
            }

            @Override
            public synchronized boolean equals(Object o) {
                return super.equals(o);
            }

            @Override
            public synchronized int hashCode() {
                return super.hashCode();
            }
        }.load(reader);
        return result;
    }

    private String getWAMachineUserName(String userCrn, String clusterId) {
        return String.format(DATABUS_CRN_PATTERN, Crn.fromString(userCrn).getAccountId(), clusterId);
    }

    private boolean isAltusDatabusCredentialNotFilled() {
        return StringUtils.isEmpty(altusDatabusCredentialFile) || StringUtils.isEmpty(altusDatabusCredentialProfile);
    }
}
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

// Exercises ImportCerts' region-filtering and CN/DN parsing logic, plus a real 2-cert PEM
// bundle parse, without touching a real keystore. The JDK 21 single-file source launcher
// (JEP 330) only compiles the one file you point it at, so this can't just be run on its
// own the way ImportCerts.java is at container startup -- it has to be compiled together
// with the canonical docker-common/bootstrap/ImportCerts.java, then run from the classpath:
//
//   javac -d /tmp/import-certs-test docker-common/bootstrap/ImportCerts.java docker-common/test/ImportCertsTest.java
//   java -cp /tmp/import-certs-test ImportCertsTest
public class ImportCertsTest {

    public static void main(String[] args) throws Exception {
        var results = new TestResults();
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS us-west-2 Root CA RSA2048 G1", "us-west-2"), "pod-region root kept");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS us-west-2 2019 CA", "us-west-2"),            "pod-region legacy CA kept");
        results.check(false, ImportCerts.shouldImportCert("Amazon RDS eu-west-1 Root CA RSA2048 G1", "us-west-2"),  "other-region root dropped");
        results.check(false, ImportCerts.shouldImportCert("Amazon RDS sa-east-1 2019 CA", "us-west-2"),             "other-region legacy dropped");
        results.check(false, ImportCerts.shouldImportCert("Amazon RDS us-west-1 Root CA RSA2048 G1", "us-west-2"),  "adjacent region not matched");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS Root 2019 CA", "us-west-2"),                  "global root kept");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS Beta Root 2019 CA", "us-west-2"),             "beta global root kept");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS Preview Root 2019 CA", "us-west-2"),          "preview global root kept");
        results.check(true,  ImportCerts.shouldImportCert("vault.example.internal", "us-west-2"),                   "non-RDS cert kept");
        results.check(true,  ImportCerts.shouldImportCert("HashiCorp Vault Intermediate Authority", "us-west-2"),   "non-RDS vault CA kept");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS eu-central-1 Root CA RSA2048 G1", "eu-central-1"), "eu pod keeps eu cert");
        results.check(false, ImportCerts.shouldImportCert("Amazon RDS us-west-2 Root CA RSA2048 G1", "eu-central-1"),    "eu pod drops us cert");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS eu-west-1 Root CA RSA2048 G1", null),         "unknown region keeps all");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS us-west-2 Root CA RSA2048 G1", ""),           "unknown region keeps all (2)");
        results.check(false, ImportCerts.shouldImportCert("Amazon RDS us-west-15 Root CA RSA2048 G1", "us-west-1"), "region prefix of a longer region not matched");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS us-west-1 Root CA RSA2048 G1", "us-west-1"),  "exact region at end of CN still matched");
        results.check(true,  ImportCerts.shouldImportCert("Amazon RDS us-west-15 and us-west-1 Root CA RSA2048 G1", "us-west-1"), "later valid occurrence matched past an earlier false-prefix one");

        results.check("Amazon RDS us-west-2 Root CA RSA2048 G1",
            ImportCerts.extractCn("CN=Amazon RDS us-west-2 Root CA RSA2048 G1,O=Amazon,C=US"), "extractCn parses CN out of a DN");
        results.check("Amazon RDS, Root CA",
            ImportCerts.extractCn("CN=Amazon RDS\\, Root CA,O=Amazon,C=US"), "extractCn unescapes a comma inside the CN value");

        testBundleParsing(results);

        results.printSummary();
        System.exit(results.allPassed() ? 0 : 1);
    }

    private static void testBundleParsing(TestResults results) throws Exception {
        Path bundle = Files.createTempFile("importcerts-test-bundle", ".pem");
        try {
            Files.writeString(bundle, TEST_CERT_US_WEST_2 + TEST_CERT_EU_WEST_1);
            List<X509Certificate> certs = ImportCerts.readCertificates(bundle.toFile());
            results.check(2, certs.size(), "bundle-parse: 2-cert PEM bundle yields 2 certificates");
            if (certs.size() == 2) {
                String firstCn = ImportCerts.extractCn(certs.get(0).getSubjectX500Principal().getName());
                results.check("Amazon RDS us-west-2 Root CA RSA2048 G1", firstCn, "bundle-parse: certs come back in file order");
            }
        } finally {
            Files.deleteIfExists(bundle);
        }
    }

    // Throwaway self-signed test fixtures (not used to trust anything, just to exercise
    // CertificateFactory.generateCertificates() on a real 2-cert bundle). Generating these
    // at test runtime would need either a keytool subprocess or the JDK-internal
    // sun.security.x509 API -- the latter forces --add-exports onto the javac/java
    // invocations above. Baking the PEM bytes in as literals keeps this dependency-free.
    private static final String TEST_CERT_US_WEST_2 = """
        -----BEGIN CERTIFICATE-----
        MIIB3zCCAUigAwIBAgIIFoexCFAaGWkwDQYJKoZIhvcNAQELBQAwMjEwMC4GA1UE
        AxMnQW1hem9uIFJEUyB1cy13ZXN0LTIgUm9vdCBDQSBSU0EyMDQ4IEcxMB4XDTI1
        MDEwMTAwMDAwMFoXDTM1MDEwMTAwMDAwMFowMjEwMC4GA1UEAxMnQW1hem9uIFJE
        UyB1cy13ZXN0LTIgUm9vdCBDQSBSU0EyMDQ4IEcxMIGfMA0GCSqGSIb3DQEBAQUA
        A4GNADCBiQKBgQDRinQXFQBKC1/D10Z2xNzd7tmrmD/FmMKybj5haxH4JJw1M+fe
        OACaYw42OM7Fx8x7GCzNeYh3baoGAjEJAXRDU1teR0uGKcZ4HAnW55ZJS90LlXIM
        sL0FnXCNuAwm0UqB6wfc2gkRGILlV8X70PJ7uwZ3s4bENqRgbPRciM6YowIDAQAB
        MA0GCSqGSIb3DQEBCwUAA4GBAGlMKLb8DHrz1+7RRFoEyJGrJnjLr/IVxmDYP78j
        NYfaYfaeQFwfCRVoicTQRC7iTlCLeu5A40b+MpoE9zsJUw9R8JR62zQEYOQ625Cj
        h7lxzd6Np7eDovA6RBuqie0Cnn5KjqZoq4E+r6GPBBPYtqOwcWzjZ5PuOkfqOEAX
        Quhh
        -----END CERTIFICATE-----
        """;

    private static final String TEST_CERT_EU_WEST_1 = """
        -----BEGIN CERTIFICATE-----
        MIIB3zCCAUigAwIBAgIIQbFBJF6vF6EwDQYJKoZIhvcNAQELBQAwMjEwMC4GA1UE
        AxMnQW1hem9uIFJEUyBldS13ZXN0LTEgUm9vdCBDQSBSU0EyMDQ4IEcxMB4XDTI1
        MDEwMTAwMDAwMFoXDTM1MDEwMTAwMDAwMFowMjEwMC4GA1UEAxMnQW1hem9uIFJE
        UyBldS13ZXN0LTEgUm9vdCBDQSBSU0EyMDQ4IEcxMIGfMA0GCSqGSIb3DQEBAQUA
        A4GNADCBiQKBgQCavsjcrGT99PNHvVbcNNcyq7921TFMTJckeDQuT104pCuNdFcg
        yjYynHPckHyFR7kaDXtQxT5XDOMRigtjc8Df1rg5kww9BVbvP9fo1FN2SBNFOPE4
        Zr3NrRKp0RYTAUVR/HRzG9dohnkXRiZgLLhenKc8ryNC9RTpIinmf6hXNQIDAQAB
        MA0GCSqGSIb3DQEBCwUAA4GBAIfMHb9Lsaa+Oa1Zp6QshJAWYDs6pKJ+skt8hv6V
        HSjMSDpAJp+PC0L99XtJMUJ6rXZIqBR1VSnOtt7vnLWdseKbFRb8lXH+96czkvx6
        ty6eAFkHM5PKJ1dIQ/P3dqCQ1t2c7u+lEAnFNpdPTY6OqWNDX38C0IJXUol2Vtt9
        38YG
        -----END CERTIFICATE-----
        """;

    private static final class TestResults {
        private int passed;
        private int failed;

        void check(Object expected, Object actual, String desc) {
            if (Objects.equals(expected, actual)) {
                passed++;
            } else {
                failed++;
                System.out.println("  FAIL " + desc + " expected='" + expected + "' actual='" + actual + "'");
            }
        }

        boolean allPassed() {
            return failed == 0;
        }

        void printSummary() {
            System.out.println();
            System.out.println("ImportCerts test: " + passed + " passed, " + failed + " failed");
        }
    }
}

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Replaces the old import_certs.sh + keytool-per-cert approach: AWS RDS ships a bundle of
// ~55 regional CA certs, and each keytool import used to be a separate JVM launch (~60s at
// container startup). This does the whole import in the ONE JVM launch that runs this file,
// via the KeyStore/CertificateFactory APIs directly, and skips RDS regional certs for a
// region other than the pod's own (AWS_REGION / AWS_DEFAULT_REGION) since a pod only talks
// to its own co-located backing database. Everything else (non-RDS certs, RDS global roots,
// an unrecognized region) is kept, so the trust store is never left incomplete.
//
// Per-service Docker build contexts mean this file is duplicated (byte-identical) into each
// docker-<svc>/bootstrap/ directory by docker-common/bootstrap/Sync.java; that is the single
// source of truth, checked for drift by Sync.java --check.
//
// Tested by docker-common/test/ImportCertsTest.java (compiled and run together, see that
// file's header for the exact commands).
//
// Usage:
//   java ImportCerts.java <cacerts-path> <storepass> <cert-dir> [cert-dir ...]
public class ImportCerts {

    private static final Pattern REGION_TOKEN = Pattern.compile("[a-z]{2}-[a-z]+-[0-9]+");
    // RFC 2253 DN escaping
    private static final Pattern CN_VALUE_UP_TO_UNESCAPED_COMMA = Pattern.compile("CN=((?:[^,\\\\]|\\\\.)+)");

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java ImportCerts.java <cacerts-path> <storepass> <cert-dir> [cert-dir ...]");
            System.exit(1);
        }
        String cacertsPath = args[0];
        String storepass = args[1];
        String podRegion = firstNonBlank(System.getenv("AWS_REGION"), System.getenv("AWS_DEFAULT_REGION"));
        System.out.println("Pod region for cert filtering: " + (podRegion == null ? "<unknown, importing all>" : podRegion));

        var keyStore = KeyStore.getInstance(new File(cacertsPath), storepass.toCharArray());
        for (int i = 2; i < args.length; i++) {
            if (args[i] != null && !args[i].isBlank()) {
                importDir(keyStore, new File(args[i]), podRegion);
            }
        }
        try (var out = new FileOutputStream(cacertsPath)) {
            keyStore.store(out, storepass.toCharArray());
        }
        System.out.println("Trust store updated: " + cacertsPath);
    }

    private static void importDir(KeyStore keyStore, File dir, String podRegion) throws Exception {
        if (!dir.isDirectory()) {
            System.out.println("NOT an existing directory " + dir);
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        System.out.println("Starting to process certificates in " + dir + " directory.");
        for (File f : files) {
            if (f.isFile()) {
                importFile(keyStore, f, podRegion);
            }
        }
    }

    private static void importFile(KeyStore keyStore, File f, String podRegion) throws Exception {
        List<X509Certificate> certs;
        try {
            certs = readCertificates(f);
        } catch (IOException | CertificateException e) {
            System.out.println("WARNING: Failed to read certificates from " + f + ": " + e);
            return;
        }
        System.out.println("checking file " + f + " (" + certs.size() + " certificate(s))");

        int imported = 0;
        for (int i = 0; i < certs.size(); i++) {
            X509Certificate cert = certs.get(i);
            String cn = extractCn(cert.getSubjectX500Principal().getName());
            if (shouldImportCert(cn, podRegion)) {
                String alias = addCertEntry(keyStore, f, cert, i, certs.size());
                imported++;
                System.out.println("  imported " + alias + " (" + cn + ")");
            } else {
                System.out.println("  Skipping certificate for a different region: " + cn);
            }
        }
        if (imported == 0 && !certs.isEmpty()) {
            System.out.println("WARNING: region filter matched no certificates in " + f + "; importing all");
            for (int i = 0; i < certs.size(); i++) {
                addCertEntry(keyStore, f, certs.get(i), i, certs.size());
            }
        }
    }

    private static String addCertEntry(KeyStore keyStore, File f, X509Certificate cert, int certIndex, int certCount) throws Exception {
        String alias = collisionSafeAliasAcrossCertDirs(f, certIndex, certCount);
        keyStore.setCertificateEntry(alias, cert);
        return alias;
    }

    private static String collisionSafeAliasAcrossCertDirs(File f, int certIndex, int certCount) {
        return certCount == 1 ? f.getPath() : f.getPath() + "-" + (certIndex + 1);
    }

    static List<X509Certificate> readCertificates(File f) throws IOException, CertificateException {
        var certificateFactory = CertificateFactory.getInstance("X.509");
        try (InputStream in = new FileInputStream(f)) {
            return certificateFactory.generateCertificates(in).stream()
                .map(X509Certificate.class::cast)
                .toList();
        }
    }

    static String extractCn(String subjectDn) {
        Matcher m = CN_VALUE_UP_TO_UNESCAPED_COMMA.matcher(subjectDn);
        return m.find() ? unescapeDn(m.group(1).trim()) : subjectDn;
    }

    private static String unescapeDn(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder unescaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i + 1 < value.length()) {
                unescaped.append(value.charAt(++i));
            } else {
                unescaped.append(ch);
            }
        }
        return unescaped.toString();
    }

    static boolean shouldImportCert(String cn, String podRegion) {
        if (podRegion == null || podRegion.isBlank()) {
            return true;
        }
        if (!cn.contains("Amazon RDS")) {
            return true;
        }
        if (!REGION_TOKEN.matcher(cn).find()) {
            return true;
        }
        return containsPodRegionNotAsPrefixOfLongerRegion(cn, podRegion);
    }

    private static boolean containsPodRegionNotAsPrefixOfLongerRegion(String cn, String podRegion) {
        int idx = cn.indexOf(podRegion);
        while (idx >= 0) {
            int end = idx + podRegion.length();
            if (end == cn.length() || !Character.isDigit(cn.charAt(end))) {
                return true;
            }
            idx = cn.indexOf(podRegion, idx + 1);
        }
        return false;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : null;
    }
}

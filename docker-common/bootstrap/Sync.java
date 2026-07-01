import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

// Copies the canonical docker-common/bootstrap/ImportCerts.java into each service's bootstrap
// directory. Per-service Docker build contexts (docker build . from docker-<svc>/) mean the
// shared helper must physically exist inside each service dir to be COPYable into the image.
//
// Usage (run from the repository root):
//   java docker-common/bootstrap/Sync.java          # write/refresh the copies
//   java docker-common/bootstrap/Sync.java --check  # exit non-zero if any copy is stale
public class Sync {

    private static final List<String> SERVICES = List.of(
        "redbeams", "cloudbreak", "datalake", "freeipa",
        "environment", "externalized-compute", "environment-remote");

    public static void main(String[] args) throws Exception {
        boolean check = args.length > 0 && args[0].equals("--check");
        Path canonical = Path.of("docker-common/bootstrap/ImportCerts.java");
        if (Files.notExists(canonical)) {
            System.err.println("Run this from the repository root; not found: " + canonical.toAbsolutePath());
            System.exit(1);
        }

        boolean stale = false;
        for (String svc : SERVICES) {
            Path dest = Path.of("docker-" + svc + "/bootstrap/ImportCerts.java");
            if (check) {
                if (Files.notExists(dest) || Files.mismatch(canonical, dest) != -1L) {
                    System.out.println("STALE: " + dest + " differs from canonical (run: java docker-common/bootstrap/Sync.java)");
                    stale = true;
                }
            } else {
                Files.copy(canonical, dest, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("synced: " + dest);
            }
        }
        System.exit(check && stale ? 1 : 0);
    }
}

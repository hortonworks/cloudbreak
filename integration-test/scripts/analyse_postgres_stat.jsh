enum Unit {
    kB,
    MB,
    GB
}

class Traffic {
    double kiloBytes;

    Traffic(String value) {
        Unit unit;
        if(value.endsWith("kB")) {
            unit = Unit.kB;
        } else if(value.endsWith("MB")) {
            unit = Unit.MB;
        } else if(value.endsWith("GB")) {
            unit = Unit.GB;
        } else {
            throw new IllegalArgumentException("No unit is present. Value: " + value);
        }
        double factor = factor(unit);
        double bytes = Double.parseDouble(value.replace(unit.name(), ""));
        this.kiloBytes = bytes * factor;
    }

    double factor(Unit unit) {
        switch(unit) {
            case kB:
                return 1;
            case MB:
                return 1024.0;
            case GB:
                return 1024.0 * 1024.0;
            default:
                throw new IllegalArgumentException("Unknown unit: " + unit);
        }
    }

    double getKiloBytes() {
        return kiloBytes;
    }

    double getAsGigaBytes() {
        return kiloBytes / factor(Unit.GB);
    }

    boolean isSmallerThan(Traffic traffic) {
        return this.kiloBytes <= traffic.getKiloBytes();
    }
}

try {
    List<String> pgStat = Files.readAllLines(Paths.get("/tmp/pg_stat_network_io.result"));

    if (pgStat.size() != 1) {
        throw new IllegalArgumentException("Expected one line for postgres network statistics. Got:" + pgStat);
    }

    String[] networkIOStat = pgStat.get(0).split("/");

    if(networkIOStat.length != 2) {
        throw new IllegalArgumentException("Expected two params from postgres network I/O separated by /. Got:" + pgStat);
    }

    String networkOutput = networkIOStat[1].trim();
    Traffic maxAllowedPostgresNetworkOutput = new Traffic(System.getenv("INTEGRATION_TEST_MAX_POSTGRES_OUTPUT").trim());
    Traffic actualPostgresNetworkOutput = new Traffic(networkOutput);

    if (actualPostgresNetworkOutput.isSmallerThan(maxAllowedPostgresNetworkOutput)) {
        System.out.print("POSTGRES>> OK");
    } else {
        System.out.println("POSTGRES>> Max allowed postgres network output: " + maxAllowedPostgresNetworkOutput.getAsGigaBytes() + "GB");
        System.out.println("POSTGRES>> Actual postgres network output: " + actualPostgresNetworkOutput.getAsGigaBytes() + "GB");
    }
} catch(Exception ex) {
    System.err.println("POSTGRES>> " + ex.getMessage());
}

/exit

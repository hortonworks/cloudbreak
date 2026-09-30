{#- CB-34129: cdp-telemetry 1.3.15 (1.3.15_b2 in prod) bundles a libgcc_s.so.1 that needs
    GLIBC_2.35, absent on RHEL 9 (2.34), so the frozen binary aborts at import and every
    cdp-doctor / cdp-telemetry call on the node fails. Dropping the bundled copy makes it fall
    back to the OS one. Fixed at build time by cdp-infra-tools PR 163; this heals nodes that
    already have a broken bundle installed.

    Include this from every entry point that shells out to cdp-telemetry, and list it *after*
    telemetry.upgrade where that is also included: upgrade_cdp_infra_tools_components reinstalls
    the RPM and would restore the broken bundle. Salt de-duplicates includes, so listing it in
    several states that end up in one run is harmless. #}
remove_cdp_telemetry_bundled_libgcc:
    file.absent:
        - name: /opt/cdp-telemetry/bin/libgcc_s.so.1
        - onlyif: test -f /lib64/libgcc_s.so.1

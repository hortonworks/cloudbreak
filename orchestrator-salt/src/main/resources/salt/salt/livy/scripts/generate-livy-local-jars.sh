#!/usr/bin/env bash

# Workaround of local livy jars for templates without HDFS until OPSAPS-78927 is resolved.
# Generates /var/lib/livy-local-jars/{rsc,repl}/ with version-stripped symlinks to the current CDH parcel's Livy jars.

set -euo pipefail

PARCEL_LIB=/opt/cloudera/parcels/CDH/lib/livy3
TARGET_ROOT=/var/lib/livy-local-jars

# ---- host arch detection ----------------------------------------------------
# Netty classifier convention: aarch_64 (underscore), not aarch64.
detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64)  echo x86_64 ;;
        aarch64|arm64) echo aarch_64 ;;
        riscv64)       echo riscv64 ;;
        *) echo "ERROR: unsupported arch $(uname -m)" >&2; exit 1 ;;
    esac
}

HOST_ARCH=$(detect_arch)
echo "host arch: $HOST_ARCH" >&2

# Matches any jar filename ending in a known native arch classifier.
NATIVE_ARCH_RE='-(x86_64|aarch_64)\.jar$'

# Strip -<version>.jar → .jar.
strip_version() {
    sed -E 's/-[0-9][^/]*\.jar$/.jar/'
}

populate() {
    local src=$1 dst=$2
    [[ -d $src ]] || { echo "ERROR: missing $src" >&2; exit 1; }

    mkdir -p "$dst"
    find "$dst" -maxdepth 1 -type l -delete

    declare -A seen=()
    local count=0
    for jar in "$src"/*.jar; do
        [[ -e $jar ]] || continue

        local real base stripped
        real=$(readlink -f "$jar")
        base=$(basename "$real")

        # If jar has a native-arch classifier, keep only host-arch variants.
        if [[ $base =~ $NATIVE_ARCH_RE ]]; then
            if [[ $base != *"-${HOST_ARCH}.jar" ]]; then
                echo "  skip (foreign arch): $base" >&2
                continue
            fi
        fi

        stripped=$(strip_version <<<"$base")

        if [[ -n ${seen[$stripped]:-} ]]; then
            echo "  WARN: collision on '$stripped' — keeping ${seen[$stripped]}, skipping $real" >&2
            continue
        fi
        seen[$stripped]=$real
        ln -sf "$real" "$dst/$stripped"
        count=$((count + 1))
    done
    echo "  $dst: $count symlinks" >&2
}

emit_property() {
    local key=$1 dir=$2
    local paths=()
    for link in "$dir"/*.jar; do
        [[ -e $link ]] || continue
        paths+=("local:$link")
    done
    ( IFS=','; echo "$key=${paths[*]}" )
}

echo "removing previous Livy local-jar symlinks"
rm -rf ${TARGET_ROOT}/*

echo "generating Livy local-jar symlinks against $(readlink -f /opt/cloudera/parcels/CDH)" >&2
populate "$PARCEL_LIB/rsc-jars"       "$TARGET_ROOT/rsc"
populate "$PARCEL_LIB/repl_2.12-jars" "$TARGET_ROOT/repl"

echo >&2
echo "# ---------- Livy Server for Spark 3 Advanced Configuration Snippet (Safety Valve) for livy3-conf/livy.conf ----------"
emit_property livy.rsc.jars  "$TARGET_ROOT/rsc"
emit_property livy.repl.jars "$TARGET_ROOT/repl"

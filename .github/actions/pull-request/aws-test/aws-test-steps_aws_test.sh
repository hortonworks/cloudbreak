#!/bin/bash -ex

# Optional AWS smoke E2E runner for PR CI. Activated by PR labels; exits successfully
# without running tests when no smoke label is present.

main() {
set -ex -o pipefail

  # Ordered highest to lowest priority. Each label maps to a suite YAML under
  # testsuites/e2e/pull-request/<label>.yaml. If multiple labels are applied,
  # only the highest-priority one runs (datahub > datalake > environment).
  AWS_SMOKE_LABELS=(
    aws-smoke-datahub-test
    aws-smoke-datalake-test
    aws-smoke-environment-test
  )

  pr_labels=$(curl -s -H "Accept: application/json" \
    "https://github.infra.cloudera.com/api/v3/repos/cloudbreak/cloudbreak/pulls/$PULL_REQUEST_NUMBER" \
    | jq -r '[.labels[].name] | join(",")')

  present_labels=()
  for label in "${AWS_SMOKE_LABELS[@]}"; do
    if [[ ",$pr_labels," == *",$label,"* ]]; then
      present_labels+=("$label")
    fi
  done

  if [[ ${#present_labels[@]} -eq 0 ]]; then
    echo "no need for build"
  else
    # present_labels preserves AWS_SMOKE_LABELS order, so index 0 is highest priority.
    selected_label="${present_labels[0]}"
    if [[ ${#present_labels[@]} -gt 1 ]]; then
      echo "Multiple smoke labels detected (${present_labels[*]}), running highest priority: ${selected_label}"
    else
      echo "${selected_label} label detected"
    fi

    export INTEGRATIONTEST_AUTHDISTRIBUTOR_HOST=thunderhead-mock
    export INTEGRATIONTEST_THREADCOUNT=4
    export RESTARTABLE_SERVICES="cloudbreak"

    export INTEGRATIONTEST_USER_ACCESSKEY="Y3JuOmFsdHVzOmlhbTp1cy13ZXN0LTE6Y2xvdWRlcmE6dXNlcjpjbG91ZGJyZWFrLXFlQHVtcy5tb2Nr"
    export INTEGRATIONTEST_USER_SECRETKEY="nHkdxgZR0BaNHaSYM3ooS6rIlpV5E+k1CIkr+jFId2g="

    # SSH private key from Vault (base action) is required for cluster SSH checks.
    # Vault holds only the private key, so the public key is derived from it (same as Harness).
    # xtrace is off here so the key material never reaches the log.
    echo "Move private key to the workspace for SSH tests"
    set +x
    mkdir -p integration-test
    PEM=integration-test/seq-master.pem
    PUB=integration-test/seq-master.pub
    printf '%s\n' "$SEQ_PRIV_FILE" | tr -d '\r' > "$PEM"
    # The key may arrive as a single-line PEM (newlines replaced by spaces), which no decoder reads.
    # Rebuild it: markers on their own lines, base64 body rewrapped at 64 columns.
    if [ "$(wc -l < "$PEM")" -le 1 ]; then
      begin=$(grep -oE -- '-----BEGIN [A-Z0-9 ]+-----' "$PEM" | head -1) || true
      end=$(grep -oE -- '-----END [A-Z0-9 ]+-----' "$PEM" | head -1) || true
      body=$(sed -E "s/.*${begin}//; s/${end}.*//" "$PEM" | tr -d ' \t\r\n')
      { printf '%s\n' "$begin"; printf '%s' "$body" | fold -w64; printf '\n%s\n' "$end"; } > "$PEM.fixed"
      mv "$PEM.fixed" "$PEM"
    fi
    chmod 600 "$PEM"
    command -v ssh-keygen >/dev/null || DEBIAN_FRONTEND=noninteractive sudo apt-get install -y openssh-client
    : > "$PUB"
    ssh-keygen -y -f "$PEM" > "$PUB" 2>/tmp/keygen.err || true
    if [ ! -s "$PUB" ]; then
      openssl rsa -in "$PEM" -pubout -out "$PUB.pem" 2>>/tmp/keygen.err \
        && ssh-keygen -f "$PUB.pem" -i -m PKCS8 > "$PUB" 2>>/tmp/keygen.err || true
    fi
    if [ ! -s "$PUB" ]; then
      echo "FATAL: could not derive SSH public key from seq-master.pem:"; cat /tmp/keygen.err
      exit 1
    fi
    set -x

    export SEQ_PUB_FILE="$PUB"
    export INTEGRATIONTEST_DEFAULTPRIVATEKEYFILE="/it/$(basename "$PEM")"
    export INTEGRATIONTEST_SSHPUBLICKEY="$(cat "$SEQ_PUB_FILE")"

    echo "will trigger aws build"

    export INTEGRATIONTEST_SUITE_FILES="file:/it/src/main/resources/testsuites/e2e/pull-request/${selected_label}.yaml"
    echo "Run this test file: $INTEGRATIONTEST_SUITE_FILES"

    # Compile artifacts only; unit tests and static analysis run in separate CI jobs.
    ./gradlew -Penv=jenkins build \
      -x test \
      -x checkstyleMain \
      -x checkstyleTest \
      -x spotbugsMain \
      -x spotbugsTest --no-daemon

    cd integration-test

    export PRIMARYKEY_CHECK=true
    echo "export CPUS_FOR_CLOUDBREAK=4.0" >> integcb/Profile_template
    # without-build: download cbd, start docker stack, run selected TestNG suite(s).
    VERSION=$(get_latest_version) TARGET_BRANCH=$BRANCH make without-build
    RESULT=$?
    # Preserve heap dumps for OOM debugging in CI artifacts.
    if [[ $(sudo find integration-test/dumps -name "*.hprof" | tail -1) ]]; then
      sudo cp -v $(sudo find integration-test/dumps -name "*.hprof" | tail -1) .
      sudo chown -R $(whoami) integration-test/dumps/*.hprof
    fi
    if [[ $RESULT -eq 0 ]]; then
      make stop-containers
    else
      exit $RESULT
    fi
  fi
}

source $(pwd)/.github/actions/pull-request/prerequisites.sh
main "$@"


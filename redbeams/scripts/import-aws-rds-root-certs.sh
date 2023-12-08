#!/bin/bash -ex

# Input:
#   $1: message
# Output:
#   -
log() {
  echo -e "$(date) $1"
}

# Input:
#   $1: message
# Output:
#   CB_WARNING_ENCOUNTERED
warn() {
  CB_WARNING_ENCOUNTERED=true
  log "WARNING: $1"
}

# Input:
#   $1: message
# Output:
#   -
abort() {
  log "ERROR: $1"
  exit 1
}

# Input:
#   CB_WARNING_ENCOUNTERED
# Output:
#   -
abort_if_warning_encountered() {
  [ -z "$CB_WARNING_ENCOUNTERED" ] || abort "Warnings have been encountered"
}

# Input:
#   CB_CERT_TMP_DIR
# Output:
#   -
cleanup_temp_dir() {
  log "Cleaning up temporary directory"
  [ -n "$CB_CERT_TMP_DIR" ] && rm -rf "$CB_CERT_TMP_DIR"
}

# Input:
#   CB_CERT_TMP_DIR
# Output:
#   -
cleanup_temp_files() {
  log "Cleaning up temporary files"
  [ -n "$CB_CERT_TMP_DIR" ] && rm -f "$CB_CERT_TMP_DIR/*"
}

# Input:
#   CB_CERT_TMP_DIR (indirectly)
# Output:
#   -
cleanup_temp_dir_upon_abort() {
  log "ERROR: Aborting"
  cleanup_temp_dir
}

# Input:
#   CB_AWS_ACCESS_KEY_ID
#   CB_AWS_SECRET_ACCESS_KEY
#   CB_AWS_GOV_ACCESS_KEY_ID
#   CB_AWS_GOV_SECRET_ACCESS_KEY
#   YQ_BINARY_PATH
#   JQ_BINARY_PATH
# Output:
#   -
validate_environment_variables() {
  log "Validating mandatory environment variables"
  local CB_MANDATORY_ENV_VARS=("CB_AWS_ACCESS_KEY_ID" "CB_AWS_SECRET_ACCESS_KEY" "CB_AWS_GOV_ACCESS_KEY_ID" "CB_AWS_GOV_SECRET_ACCESS_KEY" "YQ_BINARY_PATH" "JQ_BINARY_PATH")
  for CB_MANDATORY_ENV_VAR in "${CB_MANDATORY_ENV_VARS[@]}"
  do
    set +x
    [ -n "${!CB_MANDATORY_ENV_VAR}" ] || abort "Mandatory environment variable $CB_MANDATORY_ENV_VAR is unset"
    set -x
  done
}

# Input:
#   -
# Output:
#   -
validate_tools() {
  log "Validating mandatory tools"
  which awk aws basename cat curl cut date grep head ls jq mkdir mktemp mv openssl rm realpath sed seq sort tac tr true wc yq || abort "Some mandatory tool is absent"

  local CB_YQ_VERSION
  CB_YQ_VERSION=$($YQ_BINARY_PATH --version | awk '{print $NF}')
  log "Detected yq version $CB_YQ_VERSION"
  [ "$CB_YQ_VERSION" == '2.4.1' ] || abort "Unsupported yq version"
}

# Input:
#   -
# Output:
#   CB_CERT_TMP_DIR
init_temporary_directory() {
  local CB_CERT_TMP_DIR_BASE
  CB_CERT_TMP_DIR_BASE=./build
  log "Temporary directory base: $CB_CERT_TMP_DIR_BASE"
  log "Temporary directory base (effective): $(realpath $CB_CERT_TMP_DIR_BASE)"
  mkdir -p "$CB_CERT_TMP_DIR_BASE"
  CB_CERT_TMP_DIR=$(mktemp -d -p "$CB_CERT_TMP_DIR_BASE" -t aws-rds-root-certs.tmp.XXXXXXXXXX)
  log "Temporary directory: $CB_CERT_TMP_DIR"
}

# Input:
#   -
# Output:
#   CB_RESOURCES_CERTS_AWS_DIR
validate_resources_certs_aws_directory() {
  CB_RESOURCES_CERTS_AWS_DIR=./src/main/resources/certs/aws
  [ -d "$CB_RESOURCES_CERTS_AWS_DIR" ] || abort "Directory not found: $CB_RESOURCES_CERTS_AWS_DIR"
  log "resources/certs/aws directory: $CB_RESOURCES_CERTS_AWS_DIR"
  log "resources/certs/aws directory (effective): $(realpath $CB_RESOURCES_CERTS_AWS_DIR)"
}

# Input:
#   -
# Output:
#   stdout: regions
determine_regions() {
  local CB_ZONES_FILE=../cloud-aws-common/src/main/resources/definitions/aws-enabled-availability-zones.json
  [ -f $CB_ZONES_FILE ] || abort "File $CB_ZONES_FILE not found"
  $JQ_BINARY_PATH '.items | .[] | .name' $CB_ZONES_FILE | tr -d '"' | sort
}

# Input:
#   CB_REGION
# Output:
#   exit code of 0 or 1
is_gov_cloud_region() {
  [[ "$CB_REGION" =~ .*-gov-.* ]]
}

# Input:
#   CB_AWS_ACCESS_KEY_ID
#   CB_AWS_SECRET_ACCESS_KEY
#   CB_AWS_GOV_ACCESS_KEY_ID
#   CB_AWS_GOV_SECRET_ACCESS_KEY
#   CB_REGION
# Output:
#   AWS_ACCESS_KEY_ID
#   AWS_SECRET_ACCESS_KEY
init_aws_cli_credentials_for_region() {
  if is_gov_cloud_region
  then
    log "Setting up AWS CLI credentials for a GovCloud region"
    set +x
    export AWS_ACCESS_KEY_ID=$CB_AWS_GOV_ACCESS_KEY_ID
    export AWS_SECRET_ACCESS_KEY=$CB_AWS_GOV_SECRET_ACCESS_KEY
    set -x
  else
    log "Setting up AWS CLI credentials for a commercial region"
    set +x
    export AWS_ACCESS_KEY_ID=$CB_AWS_ACCESS_KEY_ID
    export AWS_SECRET_ACCESS_KEY=$CB_AWS_SECRET_ACCESS_KEY
    set -x
  fi
}

# Input:
#   CB_CERT_TMP_DIR
#   directory at path $CB_CERT_TMP_DIR
#   CB_REGION
# Output:
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
query_active_certs_for_region() {
  log "Querying active RDS root certificates in the region"
  local CB_DESCRIBE_CERTIFICATES_JSON="$CB_CERT_TMP_DIR/describe-certificates.json"
  aws --region $CB_REGION rds describe-certificates > "$CB_DESCRIBE_CERTIFICATES_JSON"
  log "Raw output of aws --region $CB_REGION rds describe-certificates:\\n$(cat $CB_DESCRIBE_CERTIFICATES_JSON)"

  CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV="$CB_CERT_TMP_DIR/describe-certificates_processed.csv"
  $JQ_BINARY_PATH '.Certificates | .[] | [.ValidFrom, "PUBKEY", "FILENAME", (.Thumbprint | ascii_downcase), .CertificateIdentifier, .CustomerOverride] | join(",")' \
    "$CB_DESCRIBE_CERTIFICATES_JSON" | tr -d '"' > "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
  log "Processed output of aws --region $CB_REGION rds describe-certificates:\\n$(cat $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV)"
}

# Input:
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
# Output:
#   -
validate_original_active_certs_for_region() {
  log "Validating original describe-certificates output"
  local CB_ACTIVE_CERT_COUNT
  CB_ACTIVE_CERT_COUNT=$(grep -c ',FILENAME,' "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
  [ $CB_ACTIVE_CERT_COUNT -gt 0 ] || warn "Expecting at least one active certificate in the describe-certificates output."

  # Record format:
  # ValidFrom,PUBKEY,FILENAME,Thumbprint,CertificateIdentifier,CustomerOverride
  #
  # - PUBKEY and FILENAME are always placeholders at this point. They will be filled later in split_cert_bundle_for_region().
  # - Thumbprint is always lowercase
  # - CustomerOverride is either "false" or "true" (from JSON boolean)
  #
  # Example:
  # 2019-10-28T18:05:58+00:00,PUBKEY,FILENAME,fd6f9b67b4786176fa9355cbc1db81cc72decceb,rds-ca-2019-af-south-1,false
  [ $(cut -d ',' -f 4 "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" | sort -u | wc -l) -eq $CB_ACTIVE_CERT_COUNT ] || warn "Duplicated Thumbprint in the describe-certificates output."
  [ $(cut -d ',' -f 5 "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" | sort -u | wc -l) -eq $CB_ACTIVE_CERT_COUNT ] || warn "Duplicated CertificateIdentifier in the describe-certificates output."
  local CB_CUSTOMER_OVERRIDE_COUNT
  CB_CUSTOMER_OVERRIDE_COUNT=$(grep -c ',true$' "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
  if [ $CB_ACTIVE_CERT_COUNT -le 1 ]
  then
    [ $CB_CUSTOMER_OVERRIDE_COUNT -eq 0 ] || warn "Expecting no CustomerOverride in the describe-certificates output."
  else
    [ $CB_CUSTOMER_OVERRIDE_COUNT -le 1 ] || warn "Expecting at most one CustomerOverride in the describe-certificates output."
  fi
}

# Input:
#   CB_CERT_TMP_DIR
#   directory at path $CB_CERT_TMP_DIR
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CB_REGION
# Output:
#   CB_DEFAULT_CERTIFICATE_ID
query_default_cert_for_region() {
  log "Determining default RDS root certificate in the region"
  local CB_ACTIVE_CERT_COUNT
  CB_ACTIVE_CERT_COUNT=$(grep -c ',FILENAME,' "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
  if [ $CB_ACTIVE_CERT_COUNT -le 1 ]
  then
    log "There is at most 1 active RDS root certificate in the region, which is the default one."
    CB_DEFAULT_CERTIFICATE_ID=$(cut -d ',' -f 5 "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV")
    log "CertificateIdentifier of default RDS root certificate in the region: $CB_DEFAULT_CERTIFICATE_ID"
  else
    log "There are at least 2 active RDS root certificates in the region. Querying the default one."
    local CB_DEFAULT_CERTIFICATE_JSON="$CB_CERT_TMP_DIR/default-certificate.json"
    aws --region $CB_REGION rds modify-certificates --remove-customer-override > "$CB_DEFAULT_CERTIFICATE_JSON"
    log "Raw output of aws --region $CB_REGION rds modify-certificates --remove-customer-override:\\n$(cat $CB_DEFAULT_CERTIFICATE_JSON)"
    CB_DEFAULT_CERTIFICATE_ID=$($JQ_BINARY_PATH '.Certificate.CertificateIdentifier' "$CB_DEFAULT_CERTIFICATE_JSON" | tr -d '"')
    log "CertificateIdentifier of default RDS root certificate in the region: $CB_DEFAULT_CERTIFICATE_ID"
    grep ",$CB_DEFAULT_CERTIFICATE_ID," "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || warn "Could not find associated entry for the default CertificateIdentifier in the describe-certificates output."

    local CB_CUSTOMER_OVERRIDE_ENTRY
    CB_CUSTOMER_OVERRIDE_ENTRY=$(grep ',true$' "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
    if [ -n "$CB_CUSTOMER_OVERRIDE_ENTRY" ]
    then
      log "Found previous CustomerOverride entry:\\n$CB_CUSTOMER_OVERRIDE_ENTRY"
      local CB_CUSTOMER_OVERRIDE_ID
      CB_CUSTOMER_OVERRIDE_ID=$(echo "$CB_CUSTOMER_OVERRIDE_ENTRY" | cut -d ',' -f 5)
      log "Restoring previous CustomerOverride CertificateIdentifier: $CB_CUSTOMER_OVERRIDE_ID"
      aws --region $CB_REGION rds modify-certificates --certificate-identifier $CB_CUSTOMER_OVERRIDE_ID > /dev/null
    else
      log "No CustomerOverride to restore"
    fi
  fi
}

# Input:
#   CB_CERT_TMP_DIR
#   directory at path $CB_CERT_TMP_DIR
#   CB_REGION
# Output:
#   CB_CERT_BUNDLE
#   PEM file at path $CB_CERT_BUNDLE
download_cert_bundle_for_region() {
  if is_gov_cloud_region
  then
    log "Constructing RDS certificate bundle URL for a GovCloud region"
    local CB_CERT_BUNDLE_URL="https://truststore.pki.us-gov-west-1.rds.amazonaws.com/$CB_REGION/$CB_REGION-bundle.pem"
  else
    log "Constructing RDS certificate bundle URL for a commercial region"
    local CB_CERT_BUNDLE_URL="https://truststore.pki.rds.amazonaws.com/$CB_REGION/$CB_REGION-bundle.pem"
  fi
  CB_CERT_BUNDLE="$CB_CERT_TMP_DIR/bundle.pem"
  log "Trying to download RDS certificate bundle from $CB_CERT_BUNDLE_URL to $CB_CERT_BUNDLE"
  curl -fsSo "$CB_CERT_BUNDLE" --retry 2 "$CB_CERT_BUNDLE_URL"
  log "Properties of RDS certificate bundle file:\\n$(ls -l $CB_CERT_BUNDLE)"
}

# Input:
#   CB_CERT_BUNDLE
#   PEM file at path $CB_CERT_BUNDLE
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
# Output:
#   PEM files at path $CB_CERT_BUNDLE.*
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV updated
split_cert_bundle_for_region() {
  local CB_CERT_COUNT_IN_BUNDLE
  CB_CERT_COUNT_IN_BUNDLE=$(awk '/BEGIN CERTIFICATE/' "$CB_CERT_BUNDLE" | wc -l)
  log "Total number of certificates in the RDS certificate bundle: $CB_CERT_COUNT_IN_BUNDLE"
  [ $CB_CERT_COUNT_IN_BUNDLE -gt 0 ] || warn "Expecting at least one certificate in the RDS certificate bundle."

  local CB_CERT_EXTRACTED
  local CB_CERT_EXTRACTED_FILENAME
  local CB_CERT_FINGERPRINT
  local CB_OPENSSL_CERTOPT_FOR_PUB_KEY=no_header,no_version,no_serial,no_signame,no_validity,no_subject,no_issuer,no_sigdump,no_aux,no_extensions
  local CB_CERT_PUB_KEY_DETAILS
  local CB_CERT_PUB_KEY_ALG
  local CB_CERT_PUB_KEY_SIZE_BITS
  local CB_CERT_ENTRY
  local CB_CERT_SUBJECT

  for CB_I in $(seq $CB_CERT_COUNT_IN_BUNDLE)
  do
    CB_CERT_EXTRACTED="$CB_CERT_BUNDLE.$CB_I"
    CB_CERT_EXTRACTED_FILENAME=$(basename "$CB_CERT_EXTRACTED")
    awk -v desired=$CB_I \
      'BEGIN{count=0;found=0;desired+=0}
      /BEGIN CERTIFICATE/{count++;if(count==desired){found=1}}
      (found==1){print}
      /END CERTIFICATE/ && (found==1){found=0}' "$CB_CERT_BUNDLE" > "$CB_CERT_EXTRACTED"
    CB_CERT_SUBJECT=$(openssl x509 -noout -subject -inform pem -in "$CB_CERT_EXTRACTED")
    CB_CERT_FINGERPRINT=$(openssl x509 -noout -fingerprint -sha1 -inform pem -in "$CB_CERT_EXTRACTED" | cut -d '=' -f 2 | tr -d ':' | awk '{print tolower($1)}')
    CB_CERT_PUB_KEY_DETAILS=$(openssl x509 -noout -text -inform pem -in "$CB_CERT_EXTRACTED" -certopt $CB_OPENSSL_CERTOPT_FOR_PUB_KEY)
    CB_CERT_PUB_KEY_ALG=$(echo "$CB_CERT_PUB_KEY_DETAILS" | grep 'Public Key Algorithm:' | awk '{print $NF}')
    CB_CERT_PUB_KEY_SIZE_BITS=$(echo "$CB_CERT_PUB_KEY_DETAILS" | grep 'bit)' | awk '{print $(NF - 1)}' | tr -d '(')
    log "Subject of certificate #$CB_I in the RDS certificate bundle: $CB_CERT_SUBJECT"
    log "Fingerprint of certificate #$CB_I in the RDS certificate bundle: $CB_CERT_FINGERPRINT"
    log "Public key details of certificate #$CB_I in the RDS certificate bundle:\\n$CB_CERT_PUB_KEY_DETAILS"
    log "Public key algorithm & size of certificate #$CB_I in the RDS certificate bundle: $CB_CERT_PUB_KEY_ALG, $CB_CERT_PUB_KEY_SIZE_BITS bits"
    CB_CERT_ENTRY=$(grep ",$CB_CERT_FINGERPRINT," "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
    if [ -n "$CB_CERT_ENTRY" ]
    then
      log "Found matching entry in the describe-certificates output for the fingerprint of certificate #$CB_I:\\n$CB_CERT_ENTRY"
      [ $(echo "$CB_CERT_ENTRY" | awk 'NF > 0' | wc -l) -eq 1 ] || warn "More than one matching entries. Duplicated Thumbprint in the describe-certificates output?"
      [[ "$CB_CERT_ENTRY" =~ .*,FILENAME,.* ]] || warn "Missing placeholder FILENAME in entry. Duplicated fingerprint in the RDS certificate bundle?"
      [[ "$CB_CERT_ENTRY" =~ .*,PUBKEY,.* ]] || warn "Missing placeholder PUBKEY in entry. Duplicated fingerprint in the RDS certificate bundle?"
      sed -i -e "s/,PUBKEY,FILENAME,$CB_CERT_FINGERPRINT,/,$CB_CERT_PUB_KEY_ALG-$CB_CERT_PUB_KEY_SIZE_BITS,$CB_CERT_EXTRACTED_FILENAME,$CB_CERT_FINGERPRINT,/" "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
      CB_CERT_ENTRY=$(grep ",$CB_CERT_FINGERPRINT," "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
      log "Updated matching entry in the describe-certificates output for the fingerprint of certificate #$CB_I:\\n$CB_CERT_ENTRY"
      # Example record after updates:
      # 2019-10-28T18:05:58+00:00,rsaEncryption-2048,bundle.pem.1,fd6f9b67b4786176fa9355cbc1db81cc72decceb,rds-ca-2019-af-south-1,false
      #
      # PUBKEY constructed as ALG-SIZE, where ALG is the public key algorithm and SIZE the key size in bits, both exactly as reported by openssl.
      # Some typical values:
      #
      # - id-ecPublicKey-384
      # - rsaEncryption-2048
      # - rsaEncryption-4096
      #
      # FILENAME is the PEM file (of a single cert) without any path, interpreted relative to $CB_CERT_TMP_DIR.
    else
      log "No matching entry in the describe-certificates output for certificate #$CB_I, likely an intermediate CA."
    fi
  done

  log "Properties of individual certificates from the RDS certificate bundle file:\\n$(ls -l $CB_CERT_BUNDLE.*)"
}

# Input:
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CB_REGION
# Output:
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV updated
validate_updated_active_certs_for_region() {
  log "Processed output of aws --region $CB_REGION rds describe-certificates after updates from the RDS certificate bundle:\\n$(cat $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV)"
  log "Validating updated describe-certificates output"
  if grep ',FILENAME,' "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
  then
    warn "Unresolved placeholder FILENAME in the describe-certificates output. No associated certificate in the RDS certificate bundle? Ignoring these entries."
    sed -i -e '/,FILENAME,/d' "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
    log "Processed & cleaned-up output of aws --region $CB_REGION rds describe-certificates after updates from the RDS certificate bundle:\\n$(cat $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV)"
  fi
}

# Input:
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CB_DEFAULT_CERTIFICATE_ID
#   CB_REGION
# Output:
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV updated
reorder_active_certs_for_region() {
  log "Reordering updated describe-certificates output"
  local CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV_TMP="$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV.tmp"
  mv "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV_TMP"
  # Sort first by ValidFrom timestamp, then by public key algorithm & size. Both sorting keys use ascending order & ASCII lexicographical string comparison.
  sort -o "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" -t ',' -k 1,1 -k 2,2 "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV_TMP"
  log "Processed output of aws --region $CB_REGION rds describe-certificates after updates from the RDS certificate bundle and sorting:\\n$(cat $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV)"

  # Move the entry of the default certificate to the end (so that it will later receive the largest version number)
  local CB_DEFAULT_CERTIFICATE_ENTRY
  CB_DEFAULT_CERTIFICATE_ENTRY=$(grep ",$CB_DEFAULT_CERTIFICATE_ID," "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV")
  sed -i -e "/,$CB_DEFAULT_CERTIFICATE_ID,/d" "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
  echo "$CB_DEFAULT_CERTIFICATE_ENTRY" >> "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
  log "Processed output of aws --region $CB_REGION rds describe-certificates after updates from the RDS certificate bundle, sorting and default certificate relocation:\\n$(cat $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV)"
}

# Input:
#   CB_REGION
#   CB_RESOURCES_CERTS_AWS_DIR
# Output:
#   CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   directory at path $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
init_resources_certs_aws_directory_for_region() {
  CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION="$CB_RESOURCES_CERTS_AWS_DIR/$CB_REGION"
  log "resources/certs/aws directory for the region: $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION"
  [ -d "$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION" ] || log "Directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION does not exist. Creating it."
  mkdir -p "$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION"
}

# Input:
#   $1: YAML cert filename
#   $2: YAML cert file path
# Output:
#   CB_CERT_FINGERPRINT
read_or_compute_existing_yaml_cert_fingerprint() {
  local CB_CURRENT_CERT_FILENAME="$1"
  local CB_CURRENT_CERT_PATH="$2"
  CB_CERT_FINGERPRINT=$($YQ_BINARY_PATH read "$CB_CURRENT_CERT_PATH" fingerprint)
  if [ "$CB_CERT_FINGERPRINT" == 'null' ]
  then
    CB_CERT_FINGERPRINT=$($YQ_BINARY_PATH read "$CB_CURRENT_CERT_PATH" cert | openssl x509 -noout -fingerprint -sha1 -inform pem | cut -d '=' -f 2 | tr -d ':' | awk '{print tolower($1)}')
    $YQ_BINARY_PATH write --inplace "$CB_CURRENT_CERT_PATH" fingerprint "$CB_CERT_FINGERPRINT"
    log "Calculated missing fingerprint for existing certificate $CB_CURRENT_CERT_FILENAME: $CB_CERT_FINGERPRINT"
  else
    log "Read fingerprint from existing certificate $CB_CURRENT_CERT_FILENAME: $CB_CERT_FINGERPRINT"
  fi
}

# Input:
#   $1: YAML cert filename
#   $2: YAML cert file path
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
# Output:
#   CB_CERT_FINGERPRINT
validate_existing_yaml_cert() {
  local CB_CURRENT_CERT_FILENAME="$1"
  local CB_CURRENT_CERT_PATH="$2"
  read_or_compute_existing_yaml_cert_fingerprint "$CB_CURRENT_CERT_FILENAME" "$CB_CURRENT_CERT_PATH"
  local CB_DEPRECATED_FLAG_FROM_YAML
  CB_DEPRECATED_FLAG_FROM_YAML=$($YQ_BINARY_PATH read "$CB_CURRENT_CERT_PATH" deprecated)

  local CB_CERT_ENTRY
  CB_CERT_ENTRY=$(grep ",$CB_CERT_FINGERPRINT," "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV" || true)
  if [ -n "$CB_CERT_ENTRY" ]
  then
    log "Found matching entry in the describe-certificates output for the fingerprint of existing certificate $CB_CURRENT_CERT_FILENAME:\\n$CB_CERT_ENTRY"
    [ $(echo "$CB_CERT_ENTRY" | awk 'NF > 0' | wc -l) -eq 1 ] || warn "More than one matching entries. Duplicated Thumbprint in the describe-certificates output?"

    local CB_CERTIFICATE_ID_FROM_ENTRY
    CB_CERTIFICATE_ID_FROM_ENTRY=$(echo "$CB_CERT_ENTRY" | cut -d ',' -f 5)
    local CB_CERTIFICATE_ID_FROM_YAML
    CB_CERTIFICATE_ID_FROM_YAML=$($YQ_BINARY_PATH read "$CB_CURRENT_CERT_PATH" name)
    [ "$CB_CERTIFICATE_ID_FROM_YAML" == "$CB_CERTIFICATE_ID_FROM_ENTRY" ] || warn "Mismatching name (ID) in existing certificate $CB_CURRENT_CERT_FILENAME: expected '$CB_CERTIFICATE_ID_FROM_ENTRY', found '$CB_CERTIFICATE_ID_FROM_YAML'"

    [ "$CB_DEPRECATED_FLAG_FROM_YAML" == 'null' -o "$CB_DEPRECATED_FLAG_FROM_YAML" == 'false' ] || warn "Existing certificate $CB_CURRENT_CERT_FILENAME used to be deprecated but is active again. Inconsistent describe-certificates output?"

    log "Removing active existing certificate $CB_CURRENT_CERT_FILENAME. It will be re-imported later."
    rm "$CB_CURRENT_CERT_PATH"
  else
    if [ "$CB_DEPRECATED_FLAG_FROM_YAML" == 'true' ]
    then
      log "No matching entry in the describe-certificates output for existing certificate $CB_CURRENT_CERT_FILENAME. It was already deprecated, keeping it so."
    else
      log "No matching entry in the describe-certificates output for existing certificate $CB_CURRENT_CERT_FILENAME. It used to be active, marking it as deprecated."
      $YQ_BINARY_PATH write --inplace "$CB_CURRENT_CERT_PATH" deprecated true
    fi
  fi
}

# Input:
#   CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   directory at path $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
# Output:
#   CB_NEW_CERT_START_VERSION
#   YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION updated / deleted
check_existing_certs_in_resources_certs_aws_directory_for_region() {
  local CB_CERTS_IN_RESOURCES
  CB_CERTS_IN_RESOURCES=$(ls "$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION" | grep -E '.+\.yml' || true)
  local CB_CERT_COUNT_IN_RESOURCES
  CB_CERT_COUNT_IN_RESOURCES=$(echo "$CB_CERTS_IN_RESOURCES" | awk 'NF > 0' | wc -l)
  log "Found $CB_CERT_COUNT_IN_RESOURCES existing certificate YAMLs in the resources directory:\\n$CB_CERTS_IN_RESOURCES"

  CB_NEW_CERT_START_VERSION=0
  log "Initializing CB_NEW_CERT_START_VERSION=$CB_NEW_CERT_START_VERSION"
  if [ $CB_CERT_COUNT_IN_RESOURCES -gt 0 ]
  then
    local CB_CURRENT_CERT_FILENAME
    local CB_CURRENT_CERT_PATH
    log "Checking existing certificate YAMLs in the resources directory"
    CB_NEW_CERT_START_VERSION=$(echo "$CB_CERTS_IN_RESOURCES" | sort -t '.' -k '1n,1n' | head -n 1 | cut -d '.' -f 1)
    log "Updating CB_NEW_CERT_START_VERSION=$CB_NEW_CERT_START_VERSION to minimal version of existing certificate YAMLs"
    for CB_CURRENT_CERT_FILENAME in $CB_CERTS_IN_RESOURCES
    do
      CB_CURRENT_CERT_PATH="$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION/$CB_CURRENT_CERT_FILENAME"
      validate_existing_yaml_cert "$CB_CURRENT_CERT_FILENAME" "$CB_CURRENT_CERT_PATH"
    done
  fi
}

# Input:
#   CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   directory at path $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   (deprecated) YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
# Output:
#   CB_NEW_CERT_START_VERSION
#   (deprecated) YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION renamed
rename_deprecated_certs_in_resources_certs_aws_directory_for_region() {
  local CB_CERTS_IN_RESOURCES
  # Processing deprecated YAML certs in descending order of their versions (= name without the extension, interpreted as a signed integer)
  # sort -r does not seem to work for numerical keys
  CB_CERTS_IN_RESOURCES=$(ls "$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION" | grep -E '.+\.yml' | sort -t '.' -k '1n,1n' | tac)
  local CB_CERT_COUNT_IN_RESOURCES
  CB_CERT_COUNT_IN_RESOURCES=$(echo "$CB_CERTS_IN_RESOURCES" | awk 'NF > 0' | wc -l)
  log "Found $CB_CERT_COUNT_IN_RESOURCES existing deprecated certificate YAMLs in the resources directory:\\n$CB_CERTS_IN_RESOURCES"

  if [ $CB_CERT_COUNT_IN_RESOURCES -gt 0 ]
  then
    local CB_CURRENT_CERT_FILENAME
    log "Renaming existing deprecated certificate YAMLs in the resources directory so that their versions constitute a contiguous interval"
    local CB_CERT_TARGET_VERSION
    CB_CERT_TARGET_VERSION=$(echo "$CB_CERTS_IN_RESOURCES" | head -n 1 | cut -d '.' -f 1)
    log "Maximal version of existing deprecated certificate YAMLs: $CB_CERT_TARGET_VERSION"
    CB_NEW_CERT_START_VERSION=$((CB_CERT_TARGET_VERSION + 1))
    log "Updating CB_NEW_CERT_START_VERSION=$CB_NEW_CERT_START_VERSION to maximal version + 1 of existing deprecated certificate YAMLs"
    for CB_CURRENT_CERT_FILENAME in $CB_CERTS_IN_RESOURCES
    do
      local CB_TARGET_CERT_FILENAME=$CB_CERT_TARGET_VERSION.yml
      if [ "$CB_CURRENT_CERT_FILENAME" == "$CB_TARGET_CERT_FILENAME" ]
      then
        log "Existing deprecated certificate $CB_CURRENT_CERT_FILENAME has the expected name, no renaming necessary"
      else
        local CB_CURRENT_CERT_PATH="$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION/$CB_CURRENT_CERT_FILENAME"
        local CB_TARGET_CERT_PATH="$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION/$CB_TARGET_CERT_FILENAME"
        log "Renaming deprecated certificate $CB_CURRENT_CERT_FILENAME to $CB_TARGET_CERT_FILENAME"
        mv "$CB_CURRENT_CERT_PATH" "$CB_TARGET_CERT_PATH"
      fi
      CB_CERT_TARGET_VERSION=$((CB_CERT_TARGET_VERSION - 1))
    done
  fi
}

# Input:
#   CB_CERT_TMP_DIR
#   directory at path $CB_CERT_TMP_DIR
#   CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CSV file at path $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV
#   CB_NEW_CERT_START_VERSION
#   CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   directory at path $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
# Output:
#   (active) YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION added
import_into_resources_certs_aws_directory_for_region() {
  log "Importing all active certificates from the describe-certificates output into the resources directory"
  log "Properties of files in the temporary directory before importing:\\n$(ls -l $CB_CERT_TMP_DIR)"
  local CB_CERT_ENTRY
  local CB_CERT_EXTRACTED_FILENAME
  local CB_CERT_EXTRACTED_PEM
  local CB_CERT_FINGERPRINT
  local CB_CERTIFICATE_ID_FROM_ENTRY
  local CB_CERT_TARGET_VERSION=$CB_NEW_CERT_START_VERSION
  # Import entries of $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV in the order established earlier; see reorder_active_certs_for_region().
  # The default certificate is the last one, so that it will receive the largest version number.
  while IFS= read -r CB_CERT_ENTRY
  do
    CB_CERT_EXTRACTED_FILENAME=$(echo "$CB_CERT_ENTRY" | cut -d ',' -f 3)
    local CB_CERT_EXTRACTED_PATH="$CB_CERT_TMP_DIR/$CB_CERT_EXTRACTED_FILENAME"
    CB_CERT_EXTRACTED_PEM=$(cat "$CB_CERT_EXTRACTED_PATH")
    CB_CERT_FINGERPRINT=$(echo "$CB_CERT_ENTRY" | cut -d ',' -f 4)
    CB_CERTIFICATE_ID_FROM_ENTRY=$(echo "$CB_CERT_ENTRY" | cut -d ',' -f 5)
    # The following fields of $CB_CERT_ENTRY are irrelevant and need not be stored to the YAML:
    #
    # - ValidFrom (field #1): Only relevant for sorting entries within $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV, and can be extracted by the Java logic anyway
    # - PUBKEY (field #2): Only relevant for sorting entries within $CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV, and can be extracted by the Java logic anyway
    # - CustomerOverride (field #6): Valid only for the CB AWS account where the describe-certificates command was invoked for
    local CB_TARGET_CERT_FILENAME=$CB_CERT_TARGET_VERSION.yml
    local CB_TARGET_CERT_PATH="$CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION/$CB_TARGET_CERT_FILENAME"
    log "Importing active certificate into YAML $CB_TARGET_CERT_FILENAME of the resources directory, entry:\\n$CB_CERT_ENTRY"
    $YQ_BINARY_PATH new name "$CB_CERTIFICATE_ID_FROM_ENTRY" > "$CB_TARGET_CERT_PATH"
    $YQ_BINARY_PATH write --inplace -- "$CB_TARGET_CERT_PATH" cert "$CB_CERT_EXTRACTED_PEM"
    $YQ_BINARY_PATH write --inplace "$CB_TARGET_CERT_PATH" fingerprint "$CB_CERT_FINGERPRINT"
    $YQ_BINARY_PATH write --inplace "$CB_TARGET_CERT_PATH" deprecated false
    rm "$CB_CERT_EXTRACTED_PATH"
    CB_CERT_TARGET_VERSION=$((CB_CERT_TARGET_VERSION + 1))
  done < "$CB_DESCRIBE_CERTIFICATES_PROCESSED_CSV"
  log "Properties of files in the temporary directory after importing:\\n$(ls -l $CB_CERT_TMP_DIR)"
}

# Input:
#   CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   directory at path $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
#   YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION
# Output:
#   YAML files in directory $CB_RESOURCES_CERTS_AWS_DIR_FOR_REGION updated / deleted / added
process_resources_certs_aws_directory_for_region() {
  log "Processing resources/certs/aws directory for the region"
  check_existing_certs_in_resources_certs_aws_directory_for_region
  rename_deprecated_certs_in_resources_certs_aws_directory_for_region
  import_into_resources_certs_aws_directory_for_region
}

# Input:
#   CB_REGION
# Output:
#   -
process_region() {
  log "--------------------------------------"
  log "Processing region $CB_REGION"

  init_aws_cli_credentials_for_region
  query_active_certs_for_region
  validate_original_active_certs_for_region
  query_default_cert_for_region

  download_cert_bundle_for_region
  split_cert_bundle_for_region

  validate_updated_active_certs_for_region
  reorder_active_certs_for_region

  init_resources_certs_aws_directory_for_region
  process_resources_certs_aws_directory_for_region

  cleanup_temp_files
}

# Input:
#   -
# Output:
#   -
main() {
  trap cleanup_temp_dir EXIT
  trap cleanup_temp_dir_upon_abort ERR
  CB_WARNING_ENCOUNTERED=
  # Use default collation order for sort
  export LC_ALL=C
  log "$0 Start"

  validate_environment_variables
  validate_tools

  log "Working directory: $(pwd)"
  init_temporary_directory
  validate_resources_certs_aws_directory

  local CB_BROKEN_REGIONS='eu-south-2 il-central-1'
  log "Broken regions: $CB_BROKEN_REGIONS"
  local CB_ACTIVE_REGIONS
  CB_ACTIVE_REGIONS=$(determine_regions)
  log "Active regions:\\n$CB_ACTIVE_REGIONS"
  for CB_REGION in $CB_ACTIVE_REGIONS
  do
    if echo "$CB_BROKEN_REGIONS" | grep -w "$CB_REGION"
    then
      log "--------------------------------------"
      log "Skipping broken region $CB_REGION"
    else
      process_region
    fi
  done

  log "--------------------------------------"
  log "$0 Finish"
  abort_if_warning_encountered
}

main "$@"

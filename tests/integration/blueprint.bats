#!/usr/bin/env bats

load ../commands
load ../parameters

@test "Check blueprint create from url" {
  OUTPUT=$(create-blueprint from-url --name test --url "${BLUEPRINT_URL}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"blueprint created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check blueprint create from url - url does not exist" {
  OUTPUT=$(create-blueprint from-url --name test --url https://something123456789.com 2>&1 | sed -e '$!d')

  [[ "${OUTPUT}" == *"Get https://something123456789.com: dial tcp: lookup something123456789.com"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check blueprint create from url - invalid url no protocol" {
  OUTPUT=$(create-blueprint from-url --name test --url something.com 2>&1 | sed -e '$!d')

  [[ "${OUTPUT}" == *"Get something.com: unsupported protocol scheme"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check blueprint create from file" {
  OUTPUT=$(create-blueprint from-file --name test --file blueprints/test.bp 2>&1 | tail -n 2 | head -n 1)
  echo $OUTPUT

  [[ "${OUTPUT}" == *"blueprint created: aeiou"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check blueprint create from file - file does not exist" {
  OUTPUT=$(create-blueprint from-file --name testbp --file notexists.bp 2>&1 | sed -e '$!d')

  [[ "${OUTPUT}" == *"no such file or directory"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check blueprint describe result" {
  OUTPUT=$(describe-blueprint --name "${BLUEPRINT_NAME}" | jq .Name -r)

  [[ "${OUTPUT}" == "${BLUEPRINT_NAME}" ]]
}

@test "Check blueprint describe structure" {
  OUTPUT=$(describe-blueprint --name "${BLUEPRINT_NAME}" |  jq ' . | [to_entries[].key] == ["Name","Description","HDPVersion","HostgroupCount","Tags"]')

  [[ "${OUTPUT}" == "true" ]]
}

@test "Check blueprint list" {
  CB_BLUEPRINTS=$(list-blueprints | jq 'length')
  API_BLUEPRINTS=$(curl -ks "${BASE_URL}"/cb/api/v1/blueprints/user | jq 'length')

  [[ $CB_BLUEPRINTS -eq $API_BLUEPRINTS ]]
}

@test "Check blueprint delete SUCCESS" {
  OUTPUT=$(delete-blueprint --name "${BLUEPRINT_NAME}" 2>&1 | tail -n 2 | head -n 1)

  [[ "${OUTPUT}" == *"blueprint deleted, name: ${BLUEPRINT_NAME}"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}
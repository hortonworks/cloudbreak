#!/usr/bin/env bash

load ../utils/commands

@test "Check blueprint create from url" {
  OUTPUT=$(create-blueprint from-url --name cli-bp-url --url "${BLUEPRINT_URL}" 2>&1 | tail -n 2 | head -n 1)
  echo $OUTPUT

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"blueprint created: cli-bp-url"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check blueprint create from url - url does not exist" {
  OUTPUT=$(create-blueprint from-url --name test --url https://something123456789.com 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"no such host"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check blueprint create from url - invalid url no protocol" {
  OUTPUT=$(create-blueprint from-url --name test --url something.com 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"unsupported protocol scheme"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check blueprint create from file" {
  OUTPUT=$(create-blueprint from-file --name cli-bp-file --file blueprints/test.bp 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"blueprint created: cli-bp-file"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check blueprint create from file - file does not exist" {
  OUTPUT=$(create-blueprint from-file --name testbp --file notexists.bp 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"no such file or directory"* ]]
  [[ "${OUTPUT}" == *"error"* ]]
}

@test "Check blueprint describe - default blueprint" {
  OUTPUT=$(describe-blueprint --name "${BLUEPRINT_NAME}" | jq ' . | [to_entries[].key] == ["Name","Description","HDPVersion","HostgroupCount","Tags"]')

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" ==  true ]]
}

@test "Check blueprint list" {
  for OUTPUT in $(list-blueprints | jq ' .[0] | [to_entries[].key] == ["Name","Description","HDPVersion","HostgroupCount","Tags"]');
  do
    [[ "$OUTPUT" == true ]]
  done
}

@test "Check blueprint delete url" {
  OUTPUT=$(delete-blueprint --name cli-bp-file 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"blueprint deleted, name: cli-bp-file"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}

@test "Check blueprint delete file" {
  OUTPUT=$(delete-blueprint --name cli-bp-url 2>&1 | tail -n 2 | head -n 1)

  echo "${OUTPUT}" >&2

  [[ "${OUTPUT}" == *"blueprint deleted, name: cli-bp-url"* ]]
  [[ "${OUTPUT}" != *"error"* ]]
}
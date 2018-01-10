load ../commands

BLUEPRINT_URL=https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp
BLUEPRINT_FILE="test.bp"

@test "Check blueprint create from url" {
  run create-blueprint from-url --name cli-bp-url --url $BLUEPRINT_URL
  echo $output
  [ $status = 0 ]
}

@test "Check blueprint create from url - url does not exist" {
  run create-blueprint from-url --name test --url https://something123456789.com
  echo $output
  [ $status = 1 ]
}

@test "Check blueprint create from url - invalid url no protocol" {
  run create-blueprint from-url --name test --url something.com
  echo $output
  [ $status = 1 ]
}

@test "Check blueprint create from file" {
  CHECK_RESULT=$( create-blueprint from-file --name cli-bp-file --file $BLUEPRINT_FILE )
  echo $CHECK_RESULT >&2
}

@test "Check blueprint create from file - file does not exist" {
  run create-blueprint from-file --name test --file notexists.bp
  echo $output
  [ $status = 1 ]
}

@test "Check blueprint describe - default blueprint" {
  run describe-blueprint --name "26EDW-ETL: Apache Hive 1.2.1, Apache Spark 1.6"
  echo $output
  [ $(echo $output |  jq ' . | [to_entries[].key] == ["Name","Description","HDPVersion","HostgroupCount","Tags"]' ) == true ]
}

@test "Check blueprint list" {
  CHECK_RESULT=$( list-blueprints )
  echo $CHECK_RESULT >&2
}

@test "Check blueprint delete url" {
  CHECK_RESULT=$( delete-blueprint --name cli-bp-url )
  echo $CHECK_RESULT >&2
}

@test "Check blueprint delete file" {
  CHECK_RESULT=$( delete-blueprint --name cli-bp-file )
  echo $CHECK_RESULT >&2
}
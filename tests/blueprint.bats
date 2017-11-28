load commands

BLUEPRINT_URL=https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp

@test "Check blueprint create from url" {
  run create-blueprint from-url --name test --url $BLUEPRINT_URL
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
  CHECK_RESULT=$( create-blueprint from-file --name test --file test.bp )
  echo $CHECK_RESULT >&2
}

@test "Check blueprint create from file - file does not exist" {
  run create-blueprint from-file --name testbp --file notexists.bp
  echo $output
  [ $status = 1 ]
}

@test "Check blueprint describe" {
  run describe-blueprint --name testbp
  echo $output
  [ $(echo $output |  jq ' . | [to_entries[].key] == ["Name","Description","HDPVersion","HostgroupCount","Tags"]' ) == true ]
}

@test "Check blueprint list" {
  [[ $(list-blueprints | jq ' length ' ) == $( curl -ks $CLOUD_URL/cb/api/v1/blueprints/user | jq 'length' ) ]]
}

@test "Check blueprint delete" {
  CHECK_RESULT=$( delete-blueprint --name "EDW-ETL: Apache Hive 1.2.1, Apache Spark 2.1" )
  echo $CHECK_RESULT >&2
}
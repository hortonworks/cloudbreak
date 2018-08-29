#!/bin/bash -e

: ${SOURCE_DIR:=/usr/lib/hadoop/lib}
: ${STORAGE_JAR:=gcs-connector-latest-hadoop2.jar}
: ${MR_TAR_NAME:=mapreduce.tar.gz}
: ${TEZ_TAR_NAME:=tez.tar.gz}
: ${TMP_EXTRACT_DIR:=temp_extract_dir}

enable_gcs_connector_hdfs() {
  mkdir -p /opt/salt
  SOURCE_JAR="$SOURCE_DIR/$STORAGE_JAR"
  if [ ! -f "$SOURCE_JAR" ]; then
    echo 'GC storage jar not found in the source directory, downloading it to /opt/salt'
    SOURCE_JAR="/opt/salt/$STORAGE_JAR"
    curl -Lko "$SOURCE_JAR" "https://storage.googleapis.com/hadoop-lib/gcs/gcs-connector-latest-hadoop2.jar"
  fi

  hdp_version=$(ls /usr/hdp/ | head -1);
  echo "Found HDP version: $hdp_version"

  MR_TAR_PATH="/hdp/apps/$hdp_version/mapreduce/$MR_TAR_NAME"
  TEZ_TAR_PATH="/hdp/apps/$hdp_version/tez/$TEZ_TAR_NAME"

  echo "HDFS path to $MR_TAR_NAME: $MR_TAR_PATH"
  echo "HDFS path to $TEZ_TAR_NAME: $TEZ_TAR_PATH"

  cd /opt/salt
  for tar_file_path in $MR_TAR_PATH $TEZ_TAR_PATH; do
    if hadoop fs -test -e $tar_file_path; then
      tar_file=$(basename $tar_file_path)

      echo "Copying $tar_file from HDFS to local fs."
      [[ -f "/opt/salt/$tar_file" ]] && rm "/opt/salt/$tar_file"
      hadoop fs -copyToLocal "$tar_file_path" /opt/salt

      rm -rf $TMP_EXTRACT_DIR && mkdir $TMP_EXTRACT_DIR && cd $TMP_EXTRACT_DIR
      echo "Extracting $tar_file."
      tar -zxf ../$tar_file

      jarfiles=( $(find . -name "azure-storage*.jar") )
      if [ ${#jarfiles[@]} -gt 0 ]; then
        echo "Copy gcs-connector into $tar_file."
        for jarfile in ${jarfiles[@]}; do
          echo "Copy $jarfile"
          cp -f $SOURCE_JAR $(dirname ${jarfile})
        done
        echo "Removing $tar_file."
        rm -f "../$tar_file"

        echo "Creating new $tar_file with the replaced libs."
        tar -zcf "../$tar_file" $(ls)

        echo "Replacing $tar_file on HDFS."
        su -c "hadoop fs -put -f ../$tar_file $tar_file_path" hdfs
      fi

      echo "Cleaning up local fs."
      rm -f "../$tar_file"
      cd .. && rm -rf $TM_EXTRACT_DIR
    else
      echo "$tar_file_path does not exist!"
    fi
  done
}

main() {
  if [ ! -f "/var/gcs-connector-enabled" ]; then
    enable_gcs_connector_hdfs
    echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/gcs-connector-hdfs-enabled
  else
    echo "The gcs-connector.sh has been executed previously."
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"

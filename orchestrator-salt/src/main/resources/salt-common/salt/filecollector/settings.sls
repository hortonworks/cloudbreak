{% set filecollector = {} %}

{% set s3_bucket = salt['pillar.get']('filecollector:s3_bucket') %}
{% set s3_location = salt['pillar.get']('filecollector:s3_location') %}
{% set s3_region = salt['pillar.get']('filecollector:s3_region') %}
{% set adlsv2_storage_account = salt['pillar.get']('filecollector:adlsv2_storage_account') %}
{% set adlsv2_storage_container = salt['pillar.get']('filecollector:adlsv2_storage_container') %}
{% set adlsv2_storage_location = salt['pillar.get']('filecollector:adlsv2_storage_location') %}
{% set gcs_bucket = salt['pillar.get']('filecollector:gcs_bucket') %}
{% set gcs_location = salt['pillar.get']('filecollector:gcs_location') %}
{% set account_id = salt['pillar.get']('filecollector:accountId') %}
{% set destination = salt['pillar.get']('filecollector:destination') %}
{% set issue = salt['pillar.get']('filecollector:issue') %}
{% set description = salt['pillar.get']('filecollector:description') %}
{% set start_time = salt['pillar.get']('filecollector:startTime') %}
{% set end_time = salt['pillar.get']('filecollector:endTime') %}
{% set label_filter = salt['pillar.get']('filecollector:labelFilter') %}
{% set include_salt_logs = salt['pillar.get']('filecollector:includeSaltLogs') %}
{% if salt['pillar.get']('filecollector:includeSarOutput') and salt['pkg.version']('sysstat') %}
  {% set include_sar_output = True %}
{% else %}
  {% set include_sar_output = False %}
{% endif %}
{% if salt['pillar.get']('filecollector:includeNginxReport') and salt['pkg.version']('goaccess') %}
  {% set include_nginx_report = True %}
{% else %}
  {% set include_nginx_report = False %}
{% endif %}
{% set include_selinux_report = salt['pillar.get']('filecollector:includeSeLinuxReport') %}
{% set update_package = salt['pillar.get']('filecollector:updatePackage') %}
{% set skip_test_cloud_storage = salt['pillar.get']('filecollector:skipTestCloudStorage') %}
{% set additional_logs = salt['pillar.get']('filecollector:additionalLogs') %}
{% set mode = salt['pillar.get']('filecollector:mode') %}
{% set uuid = salt['pillar.get']('filecollector:uuid') %}
{% set dbus_url = salt['pillar.get']('filecollector:dbusUrl') %}
{% set dbus_s3_url = salt['pillar.get']('filecollector:dbusS3Url') %}
{% if salt['pillar.get']('filecollector:supportBundleDbusAppName') %}
  {% set support_bundle_dbus_headers = '--header unifieddiagnostics-app:' + salt['pillar.get']('filecollector:supportBundleDbusAppName') %}
{% else %}
  {% set support_bundle_dbus_headers = '' %}
{% endif %}
{% set support_bundle_dbus_stream_name = salt['pillar.get']('filecollector:supportBundleDbusStreamName') %}
{% set support_bundle_dbus_access_key = salt['pillar.get']('filecollector:supportBundleDbusAccessKey') %}
{% set support_bundle_dbus_private_key = salt['pillar.get']('filecollector:supportBundleDbusPrivateKey') %}
{% set support_bundle_dbus_access_key_type = salt['pillar.get']('filecollector:supportBundleDbusAccessKeyType', 'Ed25519') %}

{% if s3_location and not s3_region %}
  {%- set imds_token = salt.cmd.run('curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"') %}
  {%- set imds_command = 'curl -s -H "X-aws-ec2-metadata-token: ' + imds_token + '" http://169.254.169.254/latest/dynamic/instance-identity/document' %}
  {%- set instanceDetails = salt.cmd.run(imds_command) | load_json %}
  {%- set s3_region = instanceDetails['region'] %}
{% endif %}

{% if mode == 'CLOUDERA_MANAGER' %}
  {% set compressed_file_pattern = '/var/lib/filecollector/*.zip' %}
{% else %}
  {% set compressed_file_pattern = '/var/lib/filecollector/*.gz' %}
{% endif %}

{% set cloud_storage_upload_params = None %}
{% set test_cloud_storage_upload_params = None %}
{% if s3_location %}
  {% set cloud_storage_upload_params = "s3 upload -e -p '" + compressed_file_pattern + "' --location " + s3_location + " --bucket " + s3_bucket +  " --region " + s3_region %}
  {% set test_cloud_storage_upload_params = "s3 upload -e -p /tmp/.test_cloud_storage_upload.txt --location " + s3_location + " --bucket " + s3_bucket +  " --region " + s3_region %}
{% elif adlsv2_storage_location %}
  {% set cloud_storage_upload_params = "abfs upload -p '" + compressed_file_pattern + "' --location " + adlsv2_storage_location + " --account " + adlsv2_storage_account + " --container " + adlsv2_storage_container%}
  {% set test_cloud_storage_upload_params = "abfs upload -p /tmp/.test_cloud_storage_upload.txt --location " + adlsv2_storage_location + " --account " + adlsv2_storage_account + " --container " + adlsv2_storage_container%}
{% elif gcs_location %}
  {% set cloud_storage_upload_params = "gcs upload -p '" + compressed_file_pattern + "' --location " + gcs_location + " --bucket " + gcs_bucket %}
  {% set test_cloud_storage_upload_params = "gcs upload -p /tmp/.test_cloud_storage_upload.txt --location " + gcs_location + " --bucket " + gcs_bucket %}
{% endif %}

{% set skip_validation = False %}
{% if salt['pillar.get']('filecollector:skipValidation') %}
    {% set skip_validation = True %}
{% endif %}

{% set skip_workspace_cleanup_on_startup = False %}
{% if salt['pillar.get']('filecollector:skipWorkspaceCleanupOnStartup') %}
    {% set skip_workspace_cleanup_on_startup = True %}
{% endif %}

{% set proxy_full_url = None %}
{% set proxy_protocol = None %}
{% if salt['pillar.get']('proxy:host') %}
  {% set proxy_host = salt['pillar.get']('proxy:host') %}
  {% set proxy_port = salt['pillar.get']('proxy:port')|string %}
  {% set proxy_protocol = salt['pillar.get']('proxy:protocol') %}
  {% set proxy_url = proxy_protocol + "://" + proxy_host + ":" + proxy_port %}
  {% if salt['pillar.get']('proxy:user') and salt['pillar.get']('proxy:password') %}
    {% set proxy_user = salt['pillar.get']('proxy:user') %}
    {% set proxy_password = salt['pillar.get']('proxy:password') %}
    {% set proxy_full_url =  proxy_protocol + "://" + proxy_user + ":"+ proxy_password + "@" + proxy_host + ":" + proxy_port %}
  {% else %}
    {% set proxy_full_url = proxy_url %}
  {% endif %}
{% endif %}
{% set no_proxy_hosts = salt['pillar.get']('proxy:noProxyHosts') %}

{% if salt['pillar.get']('tags:Cloudera-Resource-Name') %}
   {% set resource_crn = salt['pillar.get']('tags:Cloudera-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-resource-name') %}
   {% set resource_crn = salt['pillar.get']('tags:cloudera-resource-name') %}
{% else %}
   {% set resource_crn = '' %}
{% endif %}
{% if salt['pillar.get']('tags:Cloudera-Creator-Resource-Name') %}
   {% set creator_crn = salt['pillar.get']('tags:Cloudera-Creator-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-creator-resource-name') %}
   {% set creator_crn = salt['pillar.get']('tags:cloudera-creator-resource-name') %}
{% else %}
   {% set creator_crn = '' %}
{% endif %}
{% if salt['pillar.get']('tags:Cloudera-Environment-Resource-Name') %}
   {% set environment_crn = salt['pillar.get']('tags:Cloudera-Environment-Resource-Name') %}
{% elif salt['pillar.get']('tags:cloudera-environment-resource-name') %}
   {% set environment_crn = salt['pillar.get']('tags:cloudera-environment-resource-name') %}
{% else %}
   {% set environment_crn = '' %}
{% endif %}

{% if  salt['pillar.get']('filecollector:clusterType') %}
   {% set cluster_type = salt['pillar.get']('filecollector:clusterType') %}
{% else %}
   {% set cluster_type = '' %}
{% endif %}
{% if  salt['pillar.get']('filecollector:clusterVersion') %}
   {% set cluster_version = salt['pillar.get']('filecollector:clusterVersion') %}
{% else %}
   {% set cluster_version = '' %}
{% endif %}
{% set hostname = salt['grains.get']('fqdn') %}

{% do filecollector.update({
    "destination": destination,
    "cloudStorageUploadParams": cloud_storage_upload_params,
    "testCloudStorageUploadParams": test_cloud_storage_upload_params,
    "startTime": start_time,
    "endTime": end_time,
    "issue": issue,
    "description": description,
    "labelFilter": label_filter,
    "additionalLogs": additional_logs,
    "includeSaltLogs": include_salt_logs,
    "includeSarOutput": include_sar_output,
    "includeNginxReport": include_nginx_report,
    "includeSeLinuxReport": include_selinux_report,
    "updatePackage": update_package,
    "skipValidation": skip_validation,
    "skipWorkspaceCleanupOnStartup": skip_workspace_cleanup_on_startup,
    "proxyUrl": proxy_full_url,
    "proxyProtocol": proxy_protocol,
    "noProxyHosts": no_proxy_hosts,
    "mode": mode,
    "resourceCrn": resource_crn,
    "creatorCrn": creator_crn,
    "environmentCrn": environment_crn,
    "clusterType": cluster_type,
    "clusterVersion": cluster_version,
    "hostname": hostname,
    "uuid": uuid,
    "accountId": account_id,
    "supportBundleDbusStreamName": support_bundle_dbus_stream_name,
    "supportBundleDbusHeaders": support_bundle_dbus_headers,
    "supportBundleDbusAccessKey": support_bundle_dbus_access_key,
    "supportBundleDbusPrivateKey": support_bundle_dbus_private_key,
    "supportBundleAccessKeyType": support_bundle_dbus_access_key_type,
    "compressedFilePattern": compressed_file_pattern,
    "dbusUrl": dbus_url,
    "dbusS3Url": dbus_s3_url
}) %}
RSpec.shared_context "e2e shared vars", :a => :b do
  before (:all) {
  @os_cluster_name = "cli-os-cluster"
  @os_credential_name = "cli-os-cluster"
  @cli_input_json = "../../templates/kilo-openstack-template.json"
  @aws_credential_name = "cli-aws-cred"

  @ambari_user = "admin"
  @ambari_password = 'Admin123!@#\"'

  @recipe_types = Array["pre-ambari-start", "pre-termination","post-ambari-start", "post-cluster-install"]
  @recipe_name = "cli-recipe-url"
  @recipe_url = "https://rawgit.com/hortonworks/cb-cli/master/tests/aruba/scripts/recipe.sh"
  @recipe_file = "../../recipes/echo.sh"

  @default_blueprint_name = "'EDW-ETL: Apache Hive, Apache Spark 2'"
  @blueprint_name_url = "cli-bp-url"
  @blueprint_name_file = "cli-bp-file"
  @blueprint_url = "https://rawgit.com/hortonworks/cb-cli/master/tests/aruba/blueprints/test.bp"
  @blueprint_file = "../../blueprints/test.bp"

  @image_catalog_name = "cli-cat"
  @image_catalog_name_default = "cloudbreak-default"
  @image_catalog_url = "https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json"

  @db_name = "cli-db-postgr"

  @ldap_name = "cli-ldap"
  @ldap_domain = "ad.hwx.com"
  @ldap_bind_dn = "CN=Administrator,CN=Users,DC=ad,DC=hwx,DC=com"
  @ldap_user_search_base = "CN=Users,DC=ad,DC=hwx,DC=com"
  @ldap_user_dn_pattern= "CN={0},CN=Users,DC=ad,DC=hwx,DC=com"
  @ldap_user_name_attribute = "sAMAccountName"
  @ldap_user_object_class = "person"
  @ldap_group_member_attribute = "member"
  @ldap_group_name_attribute = "cn"
  @ldap_group_object_class = "group"
  @ldap_group_search_base = "CN=Users,DC=ad,DC=hwx,DC=com"

  @proxy_name = "cli-proxy"
  @proxy_server = "1.2.3.4"
  @proxy_port = "123"
  @proxy_user = "fakeuser"
  @proxy_password = "fakepwd"

  @mpack_name = "cli-mpack"
  @mpack_url = "http://www.test.com"
  }
end
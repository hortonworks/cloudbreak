RSpec.shared_context "mock shared vars", :a => :b do
  before (:all) {
    @os_cluster_name = "os-cluster"
    @os_credential_name = "openstack"
    @cli_input_json = "../../templates/kilo-openstack-template.json"
    @aws_credential_name = "amazon"

    @os_tenant_user = "mockuser"
    @os_tenant_name = "mocktenant"
    @os_tenant_endpoint = "mockendpoint"

    @aws_role_arn = "arn:aws:iam::1234567890:role/auto-test"
    @aws_access_key = "ABCDEFGHIJKLMNO123BC"
    @aws_secret_key = "+ABcdEFGHIjKLmNo+ABc0dEF1G23HIjKLmNopqrs"

    @az_subscription_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g"
    @az_tenant_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g"
    @az_app_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g"

    @gcp_project_id = "cloudbreak"
    @gcp_service_account_id = "1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com"
    @gcp_service_account_private_key_file = "../../keys/test.p12"

    @valid_cred_json = "{\"Name\":\"test\"}"
    @invalid_cred_json = "{\"Other\":\"test\"}"

    @imagecatalog_invalid_json = "https://rawgit.com/hortonworks/cb-cli/master/tests/templates/imagecatalog_invalid.json"

  	@db_name = "mockdbname"
  	@db_url = "jdbc:postgresql://test:99/mock"
  	@db_user = "mockuser"
    @ldap_url = "ldap://mock-12345.elb.eu-west-1.amazonaws.com:89"

    @mock_password = "mockpassword"

    @mock_endpoint_setup = "/cb/api/setup"
    @mock_endpoint_reset = "/cb/api/reset"
    @mock_password = "mockpassword"

    @default_blueprint_name = "'EDW-ETL: Apache Hive, Apache Spark 2'"
    @blueprint_name_file = "cli-bp-file"
    @blueprint_url = "https://rawgit.com/hortonworks/cb-cli/master/tests/blueprints/test.bp"
    @blueprint_file = "../../blueprints/test.bp"

    @recipe_types = Array["pre-ambari-start", "pre-termination","post-ambari-start", "post-cluster-install"]
    @recipe_name = "cli-recipe-url"
    @recipe_url = "https://rawgit.com/hortonworks/cb-cli/master/tests/recipes/post-install.sh"
    @recipe_file = "../../scripts/recipe.sh"

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

    @image_catalog_name = "cli-cat"
    @image_catalog_name_default = "cloudbreak-default"
    @image_catalog_url = "https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json"

    @audit_json = "/../../../responses/audit/audit.json"
    @audit_json_single = "/../../../responses/audit/audit_single.json"
    @dl_stack_json = "/../../../responses/stacks/datalake.json"
    @os_stack_json = "/../../../responses/stacks/openstack.json"
    @env_single_json = "/../../../responses/environment/openstack.json"
  }
end

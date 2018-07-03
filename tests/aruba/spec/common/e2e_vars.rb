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
  @recipe_url = "https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh"
  @recipe_file = "../../scripts/recipe.sh" 

  @default_blueprint_name = "'EDW-ETL: Apache Hive, Apache Spark 2'"
  @blueprint_name_url = "cli-bp-url"
  @blueprint_name_file = "cli-bp-file"
  @blueprint_url = "https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp"
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


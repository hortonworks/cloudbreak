RSpec.shared_context "mock shared vars", :a => :b do
  before (:all) { @os_tenant_user = "mockuser" }
  before (:all) { @os_tenant_pwd = "mockpwd" }
  before (:all) { @os_tenant_name = "mocktenant" }
  before (:all) { @os_tenant_endpoint = "mockendpoint" } 


  before (:all) { @aws_role_arn = "arn:aws:iam::1234567890:role/auto-test" }
  before (:all) { @aws_access_key = "ABCDEFGHIJKLMNO123BC" }    
  before (:all) { @aws_secret_key = "+ABcdEFGHIjKLmNo+ABc0dEF1G23HIjKLmNopqrs" }


  before (:all) { @az_subscription_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g" }
  before (:all) { @az_tenant_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g" }    
  before (:all) { @az_app_id = "a12b1234-1234-12aa-3bcc-4d5e6f78900g" }
  before (:all) { @az_app_pwd = "mockpwd" }  


  before (:all) { @gcp_project_id = "cloudbreak" }
  before (:all) { @gcp_service_account_id = "1234567890-abcde1fghijk2lmn1o2p34q5r7stuvz@developer.gserviceaccount.com" }    
  before (:all) { @gcp_service_account_private_key_file = "../../keys/test.p12" }      


  before (:all) { @valid_cred_json = "{\"Name\":\"test\"}" }
  before (:all) { @invalid_cred_json = "{\"Other\":\"test\"}" } 

  before (:all) { @imagecatalog_invalid_json = "https://gist.githubusercontent.com/mhalmy/a206484148d0cb02085bfbd8a58af97f/raw/f9c0a0fea59a67e1e7a" +
    "80f5e7b6defd97a69f26f/imagecatalog_invalid.json" } 



  # before (:all) { @ambari_user = "admin" }    
  # before (:all) { @ambari_password = 'Admin123!@#\"' } 


  # before (:all) { @recipe_types = Array["pre-ambari-start", "pre-termination","post-ambari-start", "post-cluster-install"] }
  # before (:all) { @recipe_name = "cli-recipe-url" }
  # before (:all) { @recipe_url = "https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh" }      
  # before (:all) { @recipe_file = "../../scripts/recipe.sh" }

  # before (:all) { @default_blueprint_name = "'EDW-ETL: Apache Hive, Apache Spark 2'" }
  # before (:all) { @blueprint_name_url = "cli-bp-url" }
  # before (:all) { @blueprint_name_file = "cli-bp_file" }
  # before (:all) { @blueprint_url = "https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp" }      
  # before (:all) { @blueprint_file = "../../blueprints/test.bp" }

  # before (:all) { @image_catalog_name = "cli-cat" }
  # before (:all) { @image_catalog_name_default = "cloudbreak-default" }
  # before (:all) { @image_catalog_url = "https://s3-eu-west-1.amazonaws.com/cloudbreak-info/v2-dev-cb-image-catalog.json" }
end


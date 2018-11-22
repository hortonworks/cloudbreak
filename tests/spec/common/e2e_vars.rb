RSpec.shared_context "shared vars", :a => :b do
  before (:all) { @os_cluster_name = "cli-os-cluster" }
  before (:all) { @os_credential_name = "cli-os-cluster" }  
  before (:all) { @cli_input_json = "../../templates/kilo-openstack-template.json" } 

  before (:all) { @ambari_user = "admin" }    
  before (:all) { @ambari_password = 'Admin123!@#\"' } 


  before (:all) { @recipe_types = Array["pre-ambari-start", "pre-termination","post-ambari-start", "post-cluster-install"] }
  before (:all) { @recipe_name = "cli-recipe-url" }
  before (:all) { @recipe_url = "https://gist.githubusercontent.com/aszegedi/4fc4a6a2fd319da436df6441c04c68e1/raw/5698a1106a2365eb543e9d3c830e14f955882437/post-install.sh" }      
  before (:all) { @recipe_file = "../../recipes/recipe.sh" }

  before (:all) { @default_blueprint_name = "'EDW-ETL: Apache Hive, Apache Spark 2'" }
  before (:all) { @blueprint_name_url = "cli-bp-url" }
  before (:all) { @blueprint_name_file = "cli-bp_file" }
  before (:all) { @blueprint_url = "https://gist.githubusercontent.com/mhalmy/8309c7e4a4649fa85f38b260a38146af/raw/5c3534c7f1849ffea64a81d467d5eee801858ff7/test.bp" }      
  before (:all) { @blueprint_file = "../../blueprints/test.bp" }

  before (:all) { @image_catalog_name = "cli-cat" }
  before (:all) { @image_catalog_name_default = "cloudbreak-default" }
  before (:all) { @image_catalog_url = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-dev-cb-image-catalog.json" }
end
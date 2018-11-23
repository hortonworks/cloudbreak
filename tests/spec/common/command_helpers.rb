RSpec.shared_context "shared command helpers", :a => :b do
  before { @some_var = :some_value }

  def scale(cb, cluster_name, hostgroup, scale_range, &block)
    skip_if(cb, cluster_name, "AVAILABLE", "Test is skipped because of cluster is not AVAILABLE")       
    before_node_count = get_node_count(cb, cluster_name, hostgroup)
    expect(before_node_count).to be >= 0
     
    result = cb.cluster.scale.name(cluster_name).group_name(hostgroup).desired_node_count(before_node_count + scale_range).build            
    expect(result.exit_status).to eql 0

    result = block.call

    after_node_count = get_node_count(cb, cluster_name, hostgroup)
    expect(after_node_count).to eq(before_node_count + scale_range) 
  end

  def bp_create_describe_delete(cb, blueprint_name, &block)
    result = block.call
    expect(result.exit_status).to eql 0

    result = cb.blueprint.describe.name(blueprint_name).build 
    expect(result.exit_status).to eql 0 

    result = list_with_name_exists(blueprint_name) do
      cb.blueprint.list.build
    end
    expect(result[0]).to be_truthy       

    result = cb.blueprint.delete.name(blueprint_name).build 
    expect(result.exit_status).to eql 0        
  end 

   def recipe_create_describe_list_delete(cb, recipe_name, &block)
    result = block.call
    expect(result.exit_status).to eql 0

    result = cb.recipe.describe.name(recipe_name).build 
    expect(result.exit_status).to eql 0

    result = list_with_name_exists(recipe_name) do
      cb.recipe.list.build
    end
    expect(result[0]).to be_truthy

    result = cb.recipe.delete.name(recipe_name).build 
    expect(result.exit_status).to eql 0                
  end

  def credential_create_describe_list_delete(cb, cred_name, &block)
    result = block.call
    expect(result.exit_status).to eql 0

    result = cb.credential.describe.name(cred_name).build 
    expect(result.exit_status).to eql 0

    result = list_with_name_exists(cred_name) do
      cb.credential.list.build
    end
    expect(result[0]).to be_truthy        

    result = cb.credential.delete.name(cred_name).build 
    expect(result.exit_status).to eql 0        
  end

  def json_has_name(json, name)
    json.each do |s|
      if s["Name"] == name
        return true, json
       end
    end
    return false, json   
  end  
         

  def list_with_name_exists(name, &block) 
    json = list_parse do
      block.call  
    end
    return json_has_name(json, name)
  end

  def list_parse(&block) 
    result = block.call
    expect(result.exit_status).to eql 0     
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    return json
  end


  def get_region(result)
    expect(result.empty?).to be_falsy
    json = JSON.parse(result)
    json.each  do |a|
      return a["Name"]
    end
  end

  def create_test_file(file_name, content = "")
    f = File.open(File.dirname(__FILE__) + "/../../tmp/aruba/" + file_name,"w")
    f.write(content)
    f.close
  end

  def load_json(file_path)
    f = File.open(File.dirname(__FILE__) + file_path)
    data = ""
    f.each do |line|
      data << line
    end
    f.close
    return data  
  end    

  def get_cluster_info(cb, cluster_name)
    json = get_cluster_json(cb, cluster_name)
    expect(json.empty?).to be_falsy
      html_print do 
        puts "\nCLUSTER INFORMATION:"
        puts "Cluster Name:    " + json["name"].to_s
        puts "Stack Id:        " + json["id"].to_s + "\n"     
      end   
  end

  def parse_debug_json(str)
    return str[str.index("{")..str.rindex("}")]
  end  

  let(:shared_let) { {'arbitrary' => 'object'} } 
  subject do
    'this is the subject'
  end
end
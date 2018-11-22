class CommandBuilder
  include Aruba::Api
  alias_method :list, :method_missing

	@@cmd = ""
	def method_missing(m, *args, &block)
		tmp = CommandBuilder.new
		if (args.size > 0)
		  CommandBuilder.cmd += "--" + m.to_s + " " + args.join("") + " "
		else
          CommandBuilder.cmd += m.to_s + " "
		end
		return tmp
	end

  def build(with_print=true)
    command_to_run = CommandBuilder.cmd.gsub("_", "-")
    result = run(command_to_run)
    result.stop
    html_print do
      puts command_to_run
      if with_print
        puts result.stdout
        puts result.stderr   
      else 
        if (result.stderr.to_s.downcase.include? "error") 
          puts result.stdout
          puts result.stderr 
        end 
      end  
    end  
    CommandBuilder.cmd = "cb "     
    return result  
  end
  
  class << self
        attr_accessor :cmd
  end
end
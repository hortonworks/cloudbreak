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

  def build_old(with_print=true)
    command_to_run = CommandBuilder.cmd.gsub("_", "-")
    result = run(command_to_run)
    result.stop
    if with_print
      html_print do 
        puts command_to_run
        puts result.stdout
        puts result.stderr
      end
    elsif (result.stderr.to_s.include? "error") or (result.stderr.to_s.include? "Error") or (result.stderr.to_s.include? "ERROR")
      html_print do 
        puts command_to_run
        puts result.stdout
        puts result.stderr
      end       
    else
      html_print do 
        #puts command_to_run
      end       
    end
    CommandBuilder.cmd = "cb "     
    return result  
  end


    def build()
    command_to_run = CommandBuilder.cmd.gsub("_", "-")
    result = run(command_to_run)
    result.stop
    if (result.stderr.to_s.include? "error") or (result.stderr.to_s.include? "Error") or (result.stderr.to_s.include? "ERROR")
      html_print do 
        puts command_to_run
        puts result.stdout
        puts result.stderr
      end     
    else
      html_print do 
        puts command_to_run
      end       
    end
    CommandBuilder.cmd = "cb "     
    return result  
  end
  
  class << self
        attr_accessor :cmd
  end
end
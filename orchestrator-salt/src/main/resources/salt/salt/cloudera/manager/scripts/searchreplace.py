import sys

source_file = sys.argv[1]
target_file = sys.argv[2]

# Read lines from the source file into a list
with open(source_file, "r") as source:
    source_lines = source.readlines()

# Read lines from the target file into a list
with open(target_file, "r") as target:
    target_lines = target.readlines()

# Create a dictionary to store the source variables
source_lines_dict = { line.strip().split(None, 2)[1] : line for line in source_lines if not line.strip() == "" }

# Process the target lines, replacing or adding lines as needed
for i in range(len(target_lines)):
    target_line = target_lines[i].strip()
    if not target_line.startswith("#") and not target_line.strip() == "":
        target_variable = target_line.split(None, 2)[1]

        if target_variable in source_lines_dict.keys():
            # If a matching is found in the source, replace the target line
            target_lines[i] = source_lines_dict[target_variable]
            del source_lines_dict[target_variable]

# Append any remaining source lines (lines not matching any line in the target file)
target_lines.extend(source_lines_dict.values())

# Empty the target file
open(target_file, 'w').close()

# Write the updated target lines
with open(target_file, "w") as output:
    output.writelines(target_lines)
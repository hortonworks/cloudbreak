#!/bin/python
import sys
import os
import glob
import json
import yaml

MAX_FILE_SIZE=314572800
ONE_GB_FILE_SIZE=1073741824
FILECOLLECTOR_CONFIG="/opt/cdp-telemetry/conf/filecollector-collect.yaml"

def main(args):
    if not os.path.exists(FILECOLLECTOR_CONFIG):
        print("{}")
        return
    result={}
    labelsToKeep=['salt_master', 'salt_minion', 'salt_api', 'syslog']
    for labelToKeep in labelsToKeep:
      result[labelToKeep]=0
    skipLabels=[]
    with open(FILECOLLECTOR_CONFIG) as f:
      data = yaml.safe_load(f)
      files=data['collector']['files']
      new_files=[]
      sizes=[]
      max_label_size_map={}
      disablePerlScript=False
      hasTooLargeImportantLog=False
      for file in files:
        label=file['label']
        for c_file in glob.glob(file['path']):
          size=os.stat(c_file).st_size
          if not (label in max_label_size_map) or (label in max_label_size_map and size > max_label_size_map[label]):
            max_label_size_map[label]=size
            if label in labelsToKeep:
              result[label]=max_label_size_map[label]
            if size > MAX_FILE_SIZE:
              if not (label in labelsToKeep):
                skipLabels.append(label)
              else:
                if size < ONE_GB_FILE_SIZE:
                  disablePerlScript=True
                else:
                  hasTooLargeImportantLog=True
          if not (label in skipLabels):
            sizes.append(size)
        if not (label in skipLabels):
          new_files.append(file)
    result['skipLabels']=",".join(skipLabels) if skipLabels else ""
    top_5_size=sum(sorted(sizes[:5], reverse=True))
    free_memory=int(os.popen("free -b | awk 'NR==2{print $4}'").readlines()[-1].rstrip())
    hasRAM=True if free_memory > top_5_size else False
    result['hasEnoughFreeMemory']=hasRAM
    result['requiredFreeMemory']=top_5_size
    writeYaml=False
    if not hasRAM:
      files=data['collector']['files']
      required_files_to_keep=[]
      for file in files:
        if ("mandatory" in file and file["mandatory"]) or ("label" in file and file["label"] in ["report", "doctor"]):
          required_files_to_keep.append(file)
      data['collector']['files']=required_files_to_keep
      writeYaml=True
    elif skipLabels:
      data['collector']['files']=new_files
      writeYaml=True
    if hasRAM and disablePerlScript and not hasTooLargeImportantLog:
      data['collector']['logProcessorWorkers']=1
      data['collector']['usePerlScriptForAnonymization']=False
      writeYaml=True
    if writeYaml:
      with open(FILECOLLECTOR_CONFIG, "w") as f:
        yaml.dump(data, f)
    print(json.dumps(result))

if __name__ == "__main__":
    main(sys.argv[1:])
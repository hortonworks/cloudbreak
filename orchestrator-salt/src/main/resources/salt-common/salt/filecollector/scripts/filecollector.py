#!/usr/bin/env python

import argparse
import sys
import logging
import os
import yaml
import glob
import gzip
import shutil
import datetime
import tarfile
import fileinput
import re
import zipfile
import subprocess
import socket
import time
from fluent import sender
from fluent import event
from pid import PidFile

def parse_args(args):
    parser = argparse.ArgumentParser(
        description='Python script to collect logs to specific folder')
    parser.add_argument('--config', type=str, required=True,
                        help='Path to logcollector configuration')
    parser.add_argument('--labels', type=str, required=False,
                            help='Comma separated list of labels for filtering files for collection')
    parser.add_argument('--start-time', type=float, required=False, dest="start_time",
                        help='Start (last modified) datestamp (epoh unix format) for the monitored logs')
    parser.add_argument('--end-time', type=float, required=False, dest="end_time",
                        help='End (creation) datestamp (epoh unix format) for the monitored logs (epoh unix format)')
    args = parser.parse_args(args)
    return args

def main(args):
    args = parse_args(args)
    filteredLabels = args.labels.split(',') if args.labels else []
    startTime=args.start_time
    endTime=args.end_time
    with open(args.config) as file:
        config = yaml.load(file, yaml.SafeLoader)
        if config and "collector" in config:
            logger=__setup_logger(config)
            outputLocation=config["collector"]["outputLocation"]
            outputScript=__get_str_key("outputScript", config["collector"])
            preProcessScript=__get_str_key("preProcessScript", config["collector"])
            processFileScript=__get_str_key("processFileScript", config["collector"])
            files=config["collector"]["files"]
            useFullPath=__get_bool_key("useFullPath", config["collector"], True)
            compressFormat=__get_str_key("compressFormat", config["collector"], "zip")
            now = datetime.datetime.today()
            nTime = now.strftime("%Y-%m-%d-%H-%M-%S-%f")
            hostname=None
            if socket.gethostname().find('.')>=0:
                hostname=socket.gethostname()
            else:
                hostname=socket.gethostbyaddr(socket.gethostname())[0]
            zipfile_name = nTime + "-" + hostname.replace(".", "-")
            tmp_folder=os.path.abspath(os.path.join(outputLocation, "tmp", zipfile_name))
            if preProcessScript:
                subprocess.call([preProcessScript, tmp_folder])

            fluentEventProcessor = None
            if "fluentProcessor" in config["collector"]:
                fluent_host=__get_str_key("host", config["collector"]["fluentProcessor"], "localhost")
                fluent_port=__get_int_key("port", config["collector"]["fluentProcessor"], 24224)
                fluent_tag=__get_str_key("tag", config["collector"]["fluentProcessor"])
                additional_fields=config["collector"]["fluentProcessor"]["additionalFields"] if "additionalFields" in config["collector"]["fluentProcessor"] else {}
                additional_fields_key=__get_str_key("additionalFieldsKey", config["collector"]["fluentProcessor"], "additionalContext")
                message_field=__get_str_key("messageField", config["collector"]["fluentProcessor"], "message")
                include_time=__get_bool_key("includeTime", config["collector"]["fluentProcessor"])
                fluentEventProcessor=EventProcessor(fluent_host, int(fluent_port), fluent_tag, additional_fields, additional_fields_key, message_field, include_time)
            if not os.path.exists(tmp_folder):
                os.makedirs(tmp_folder)
            __disk_check(files, filteredLabels, outputLocation, config["collector"], startTime, endTime, logger)
            for fileObject in files:
                mandatory=bool(fileObject["mandatory"]) if "mandatory" in fileObject else False
                if not mandatory and filteredLabels and fileObject["label"] not in filteredLabels:
                    continue
                sortFilesByDate=not "sortFilesByDate" in config["collector"] or bool(config["collector"]["sortFilesByDate"])
                allfiles=sorted(glob.glob(fileObject["path"]), key=os.path.getmtime) if sortFilesByDate else glob.glob(fileObject["path"])
                exclude_files=__get_excludes(fileObject["excludes"] if "excludes" in fileObject else [], logger)
                for file in allfiles:
                    absFilePath=os.path.abspath(file)
                    if file in exclude_files:
                        continue
                    if __is_file_not_in_date_rage(absFilePath, startTime, endTime, logger):
                        continue
                    logger.debug("process file: %s" % absFilePath)
                    dest_folder=None
                    labelInPath=fileObject["label"].lower()
                    if "skipLabelFromPath" in fileObject and fileObject["skipLabelFromPath"]:
                        labelInPath=""
                    if "folderPrefix" in fileObject:
                        dest_folder=os.path.join(tmp_folder, fileObject["folderPrefix"], labelInPath)
                    else:
                        dest_folder=os.path.join(tmp_folder, labelInPath)
                    useFullPathPerFile=__get_bool_key("useFullPath", fileObject, True) if "useFullPath" in fileObject else useFullPath
                    skipAnonymization=__get_bool_key("skipAnonymization", fileObject, False)
                    dest=os.path.join(dest_folder, os.path.abspath(file).lstrip(os.sep)) if useFullPathPerFile else os.path.join(dest_folder, os.path.basename(file))
                    dest_parent=os.path.dirname(dest)
                    if not os.path.exists(dest_parent):
                        os.makedirs(dest_parent)
                    if os.path.isfile(file):
                        shutil.copy(file, dest)
                    if not skipAnonymization and "rules" in config["collector"] and config["collector"]["rules"]:
                        for line in fileinput.input(dest, inplace=1):
                            for rule in config["collector"]["rules"]:
                                line = re.sub(rule["pattern"], rule["replacement"], line.rstrip())
                            print(line)
                    if processFileScript:
                        subprocess.call([processFileScript, dest, fileObject["label"]])
                    if fluentEventProcessor:
                        fluentEventProcessor.process(fileObject["label"], os.path.abspath(file), dest)
                    deleteProcessedTempFilesOneByOne=__get_bool_key("deleteProcessedTempFilesOneByOne", config["collector"])
                    if deleteProcessedTempFilesOneByOne:
                        os.remove(dest)

            processFilesFolderScript=__get_str_key("processFilesFolderScript", config["collector"])
            if processFilesFolderScript:
                subprocess.call([processFilesFolderScript, tmp_folder])

            skip_compress=not __get_bool_key("compress", config["collector"], True)
            keep_processed_files=not __get_bool_key("deleteProcessedTempFiles", config["collector"], True)
            if skip_compress:
                print("skipping file compression")
            else:
                extension = "zip"
                if compressFormat == "tar":
                    extension = "tar"
                elif compressFormat == "gztar":
                    extension = "tar.gz"
                elif compressFormat == "bztar":
                    extension = "tar.bz2"

                output_file=os.path.join(outputLocation, zipfile_name)
                make_archive(tmp_folder, output_file, compressFormat, extension)

            if keep_processed_files:
                print("keep processed files in '%s' folder" % os.path.join(outputLocation, "tmp"))
            else:
                shutil.rmtree(os.path.join(outputLocation, "tmp"))

            if not skip_compress and outputScript:
                output_compressed_file="%s.%s" % (output_file, extension)
                subprocess.call([outputScript, output_compressed_file])
                deleteCompressedFile=__get_bool_key("deleteCompressedFile", config["collector"])
                if deleteCompressedFile:
                   os.remove(output_compressed_file)

            if fluentEventProcessor:
                fluentEventProcessor.close()

def make_archive(source, destination, format, extension):
    name = os.path.basename(destination)
    archive_from = os.path.dirname(source)
    archive_to = os.path.basename(source.strip(os.sep))
    shutil.make_archive(name, format, archive_from, archive_to)
    shutil.move("%s.%s" % (name, extension), "%s.%s" % (destination, extension))

def __get_str_key(key, map, default=None):
    if default:
        return map[key] if key in map else str(default)
    else:
        return map[key] if key in map else None

def __get_int_key(key, map, default=None):
    if default:
        return int(map[key]) if key in map else int(default)
    else:
        return map[key] if key in map else None

def __get_float_key(key, map, default=None):
    if default:
        return float(map[key]) if key in map else float(default)
    else:
        return map[key] if key in map else None

def __get_bool_key(key, map, default=False):
    return bool(map[key]) if key in map else bool(default)

def __setup_logger(config):
    logger = logging.getLogger('filecollector')
    consoleHandler = logging.StreamHandler(sys.stdout)
    logger.setLevel(logging.INFO)
    formatter = logging.Formatter('%(message)s')
    if "logger" in config["collector"]:
        logger.setLevel(__get_str_key("level", config["collector"]["logger"], "INFO"))
        formatter = logging.Formatter(__get_str_key("format", config["collector"]["logger"], "%(message)s"))
        if "file" in config["collector"]["logger"] and config["collector"]["logger"]["file"]:
            fileHandler = logging.RotatingFileHandler(config["collector"]["logger"]["file"], maxBytes=(1048576*5), backupCount=5)
            fileHandler.setFormatter(formatter)
            logger.addHandler(fileHandler)
    consoleHandler.setFormatter(formatter)
    logger.addHandler(consoleHandler)
    return logger

def __get_excludes(paths, logger):
    exclude_files=[]
    for filepath in paths:
        files=glob.glob(filepath)
        for file in files:
            exclude_files.append(file)
            logger.debug("file %s will be excluded from processing." % file)
    return exclude_files

def __disk_check(files, filteredLabels, outputLocation, config, startTime, endTime, logger):
    checkDiskSpace=__get_bool_key("checkDiskSpace", config, True)
    requiredDiskSpaceRatio=__get_float_key("requiredDiskSpaceRatio", config, 1.0)
    if checkDiskSpace:
        logger.debug("disk space check is enabled")
        fullSize=0
        for fileObject in files:
            if filteredLabels and fileObject["label"] not in filteredLabels:
                continue
            filePaths=glob.glob(fileObject["path"])
            for file in filePaths:
                if __is_file_not_in_date_rage(os.path.abspath(file), startTime, endTime, logger):
                    continue
                fullSize = fullSize + os.stat(file).st_size
        freeSpace=0
        requiredFreeSpace=0
        try:
            total, used, freeSpace = shutil.disk_usage(outputLocation)
        except Exception as e:
            import psutil
            hdd = psutil.disk_usage(outputLocation)
            freeSpace=hdd.free
        requiredFreeSpace=int(fullSize * requiredDiskSpaceRatio)
        if fullSize > freeSpace:
            logger.error("there is not enough free space for file collection - free space: %d, required space: %d" % (freeSpace, requiredFreeSpace))
            sys.exit(1)
        else:
            logger.debug("free disk space: %d, required disk space: %d" % (freeSpace, requiredFreeSpace))

    else:
        logger.debug("disk space check is disabled")

def __is_file_not_in_date_rage(file_path, start_time, end_time, logger):
    result=False
    if start_time or end_time:
        creationDate=__creation_date(file_path)
        lastModifiedDate=os.stat(file_path).st_mtime
        logger.debug("file '%s' creation date: %s." % (file_path, creationDate))
        logger.debug("file '%s' last modification date: %s." % (file_path, lastModifiedDate))
        if start_time and lastModifiedDate < (start_time / 1000.0):
            logger.debug("skipping processing file '%s' as last modification time is smaller than 'start-time' parameter" % file_path)
            result=True
        elif end_time and (end_time / 1000.0) < creationDate:
            logger.debug("skipping processing file '%s' as creation time is larger than 'end-time' parameter" % file_path)
            result=True
        else:
            logger.debug("file '%s' is in the right date range (based on creation/last modification datestamps)" % file_path)
    return result

def __creation_date(file_path):
    stat = os.stat(file_path)
    try:
        return stat.st_birthtime
    except AttributeError:
        return stat.st_mtime

class EventProcessor:

    def __init__(self, host, port, base_tag, additional_fields = {}, additional_fields_key = "additionalContext", message_field="message", include_time=False):
        self.host = host
        self.port = port
        self.base_tag = base_tag
        self.message_field = message_field
        self.include_time = include_time
        self.additional_fields = {}
        self.additional_fields_key = additional_fields_key
        if bool(additional_fields):
            self.additional_fields = {}
            for key, val in additional_fields.items():
                self.additional_fields[key]=val
        if host and port:
            self.fluentSender = sender.FluentSender(base_tag, host=host, port=port)
        else:
            self.fluentSender = sender.FluentSender(base_tag)

    def process(self, name, path, real_path):
        with open(real_path, 'r', buffering=100000) as infile:
            if "*" in name:
                replaced_path=path.replace(os.sep, ".")
                name=name.replace("*", replaced_path)
            for line in infile:
                fields = {self.message_field: line}
                if bool(self.additional_fields):
                    fields[self.additional_fields_key]=self.additional_fields
                if self.include_time:
                    self.fluentSender.emit_with_time(name, time.time(), fields)
                else:
                    self.fluentSender.emit(name, fields)

    def close(self):
        self.fluentSender.close()

if __name__ == "__main__":
    pidfile=os.environ.get('FILECOLLECTOR_PIDFILE', 'filecollector-collector.pid')
    with PidFile(pidfile) as p:
        main(sys.argv[1:])
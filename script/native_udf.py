#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import io
import os
import subprocess
import re
from collections import deque
from typing import Final
import hashlib

BASE_DIR: Final[str] = "/opt/udf-trans-opt/udf-translator/"
SCANNER_DIR: Final[str] = "/opt/udf-trans-opt/"
MIN_PYTHON = (3, 6)
if sys.version_info < MIN_PYTHON:
    sys.exit(
        f"Python {MIN_PYTHON[0]}.{MIN_PYTHON[1]}+ version require\n"
        f"current version{sys.version.split()[0]}"
    )

sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding='utf-8')
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', write_through=True)
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', write_through=True)

def main():
    if len(sys.argv) != 3:
        print("Usage: python native_udf.py <command> <jar_path>")
        print("Available commands:")
        print("  list  - success native udf message")
        print("  source_info    - success native udf source code")
        print("  depend_info  - get dependencies")
        print("  fail_info  - get fail message")
        print("  tune_level  - change native udf tune level")
        sys.exit(1)

    command = sys.argv[1].lower()
    option = sys.argv[2]

    try:
        if command == "list":
            list_udf(option)
        elif command == "source_info":
            source_info(option)
        elif command == "depend_info":
            depend_info(option)
        elif command == "fail_info":
            fail_info(option)
        elif command == "tune_level":
            udf_tune(option)
        else:
            print(f"invalid command: {command}")
            print("valid command: list, source_info, depend_info, fail_info")
            sys.exit(1)
    except Exception as e:
        print(f"failed: {str(e)}")
        sys.exit(1)

def get_hash(file_path):
    hash_file = os.path.join(BASE_DIR, "hash_record.txt")
    try:
        with open(hash_file, 'r') as f:
            lines=f.readlines()
            for content in lines:
                if content.startswith(file_path):
                    words = content.split(":")
                    return words[1]
        return "null"
    except Exception as e:
        raise Exception(f"failed to get jarHash: {str(e)}")

def list_udf(file_path):
    hash = get_hash(file_path)
    hash_output_path = os.path.join(BASE_DIR, "output", hash)
    if not os.path.exists(hash_output_path):
        print("failed")
        return
    prop_file = os.path.join(hash_output_path, "udf.properties")
    try:
        print("udf native success, here is the map:\n")
        with open(prop_file, 'r', encoding='utf-8') as f:
            print(f.read())
            print("\n")
    except FileNotFoundError:
        print(f"error: {prop_file} doesn't exist\n")
    except Exception as e:
        print(f"read {prop_file} failed: {str(e)}\n")

def source_info(file_path):
    hash = get_hash(file_path)
    hash_cpp_path = os.path.join(BASE_DIR, "cpp", hash)
    if not os.path.exists(hash_cpp_path):
        print("cpp dir doesn't exist")
        return
    print("source code path:", hash_cpp_path)
    sub_dirs = []
    print("udf share cpp files:")
    for root, dirs, files in os.walk(hash_cpp_path):
        for name in dirs:
            sub_dirs.append(name)
        for name in files:
            if name.endswith(".cpp"):
                print(name)
        break
    for sub_dir in sub_dirs:
        print("udf private files for " + sub_dir + ":")
        sub_path = os.path.join(hash_cpp_path, sub_dir)
        for sub_root, sub_dirs, sub_files in os.walk(sub_path):
            for sub_name in sub_files:
                if sub_name.endswith(".cpp"):
                    print(os.path.join(sub_dir,sub_name))
            break

def depend_info(file_path):
    try:
        jar_path = os.path.join(SCANNER_DIR, "unt-scanner-1.0-bin.jar")
        txt_path = os.path.join(BASE_DIR, "DependencyScanResult.txt")

        if not os.path.exists(jar_path):
            raise FileNotFoundError(f"untScanner not found: {jar_path}")

        subprocess.run(
            ["java", "-jar", jar_path, file_path],
            check=True,
            cwd=BASE_DIR,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )

        if os.path.exists(txt_path):
            with open(txt_path, "r", encoding="utf-8") as f:
                print("\n" + "="*40 + " DependencyScanResult: " + "="*40)
                print(f.read())
                print("="*40 + " end " + "="*40 + "\n")

            print(f"[SUCCESS] result has been saved in: {txt_path}")
            return True
        else:
            raise FileNotFoundError("untScanner-1.2-bin.jar doesn't generate DependencyScanResult.txt")

    except subprocess.CalledProcessError as e:
        print(f"[ERROR] exec untScanner-1.2-bin.jar failed: {e.stderr}")
    except Exception as e:
        print(f"[ERROR] error: {str(e)}")

    return False

def fail_info(file_path):
    try:
        error_entries = analyze_error_logs(file_path)
    except FileNotFoundError:
        print("[] log file not found")
        return
    if not error_entries:
        print("no error logs found in unt.log")
        return

    COLORS = {
        "red": "\033[91m",
        "green": "\033[92m",
        "yellow": "\033[93m",
        "cyan": "\033[96m",
        "reset": "\033[0m"
    }

    print(f"\n{COLORS['red']} find {len(error_entries)} errors info{COLORS['reset']}")

    for idx, error in enumerate(error_entries, 1):
        print(f"\n{COLORS['cyan']} error #{idx} {COLORS['reset']}")
        print(f"{COLORS['yellow']} time:{COLORS['reset']} {error['timestamp']}")
        print(f"{COLORS['yellow']} info:{COLORS['reset']}")

        for i, line in enumerate(error['message']):
            prefix = "" if i == len(error['message'])-1 else ""
            print(f"  {COLORS['green']}{prefix} {line}{COLORS['reset']}")

    print(f"\n{COLORS['red']} finish {COLORS['reset']}")

def analyze_error_logs(file_path):
    hash = get_hash(file_path)
    log_file = os.path.join(BASE_DIR, "log", "unt.log")

    if not os.path.exists(log_file):
        raise Exception(f"log file {log_file} doesn't exist")

    log_entry_pattern = re.compile(
                            r'(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})'
                            r'\s+\[(?P<thread>.+?)\]'
                            r'\s+(?P<level>ERROR|INFO|WARN|DEBUG)\s*'
                            r'(?P<message>.*)'
                        )
    
    current_block = deque(maxlen=10000)
    found_target = False
    hash_start_pattern = re.compile(r'translate logs for jar (\w+) start')

    for line in reverse_readline(log_file):
        hash_match = hash_start_pattern.search(line)
        if hash_match:
            current_hash = hash_match.group(1)
            if current_hash == hash:
                found_target = True
                break
            current_block.clear()
            continue

        current_block.appendleft(line)

    if not found_target:
        return []

    error_entries = []
    current_error = None

    for line in current_block:
        entry_match = log_entry_pattern.match(line)
        if entry_match:
            timestamp = entry_match.group('timestamp')
            level = entry_match.group('level')
            message = entry_match.group('message')
            if current_error:
                error_entries.append(current_error)
                current_error = None
            if level == "ERROR":
                current_error = {
                    "timestamp": timestamp,
                    "message": [message]
                }
        else:
            if current_error:
                current_error["message"].append(line)

    if current_error:
        error_entries.append(current_error)

    return error_entries

def reverse_readline(filename, buf_size=8192):
    with open(filename, 'rb') as f:
        f.seek(0, 2)
        position = f.tell()
        remainder = bytearray()

        while position > 0:
            if position - buf_size < 0:
                read_size = position
                position = 0
            else:
                read_size = buf_size
                position -= buf_size

            f.seek(position)
            chunk = f.read(read_size)

            lines = chunk.split(b'\n')
            if remainder:
                lines[-1] += remainder
            remainder = lines[0]

            for line in reversed(lines[1:]):
                yield line.decode('utf-8').rstrip('\r\n')

        if remainder:
            yield remainder.decode('utf-8').rstrip('\r\n')


def udf_tune(level):
    tune_file = os.path.join(BASE_DIR, "conf", "udf_tune.properties")
    try:
        with open(tune_file, "r") as f:
            lines = f.readlines()
            modify_line_num = 0
            select_content=""
            for content in lines:
                if content.startswith("tune_level"):
                    words = content.split("=")
                    select_content = words[0] + "=" + level + "\n"
                    break
                modify_line_num += 1
            lines[modify_line_num] = select_content
        with open(tune_file, "w") as f:
            f.writelines(lines)
    except Exception as e:
        print("open tune file fail:",e)

if __name__ == "__main__":
    main()


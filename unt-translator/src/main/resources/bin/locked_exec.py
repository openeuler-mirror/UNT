#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import io
import os
import fcntl
import subprocess

MIN_PYTHON = (3, 6)
if sys.version_info < MIN_PYTHON:
    sys.exit(
        f"Python {MIN_PYTHON[0]}.{MIN_PYTHON[1]}+ version require\n"
        f"current version{sys.version.split()[0]}"
    )

sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding='utf-8')
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', write_through=True)
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', write_through=True)

LOCK_FILE = os.path.join(os.getcwd(), 'script_temp.lock')

def main():

    with open(LOCK_FILE, 'a+') as f:
        fcntl.flock(f.fileno(), fcntl.LOCK_EX)

        if len(sys.argv) < 2:
            print("Error: none command to be executed")
            sys.exit(1)

        command = ' '.join(sys.argv[1:])
        try:
            subprocess.run(command, shell=True, check=True)
        except subprocess.CalledProcessError as e:
            print(f"exec command failederrorcode: {e.returncode}")
            sys.exit(e.returncode)

if __name__ == "__main__":
    main()

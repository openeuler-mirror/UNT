/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 */

package com.huawei.unt;

import java.io.FileWriter;
import java.io.IOException;

/**
 * UNT scanner main class
 *
 * @since 2025-05-19
 */
public class UNTMain {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new UNTException("jar path is empty or too much, only support one.");
        }

        JarHandler jarHandler;
        try {
            jarHandler = new JarHandler(args[0]);
        } catch (Exception e) {
            throw new UNTException("Create jar handler failed, " + e.getMessage());
        }

        try (FileWriter writer = new FileWriter("DependencyScanResult.txt")) {
            DependencyScanner.dependencyScan(jarHandler, writer);
        } catch (IOException e) {
            throw new UNTException("Create dependency scan result file failed, " + e.getMessage());
        }
    }
}

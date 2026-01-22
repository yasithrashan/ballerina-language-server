/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.artifactsgenerator.codemap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks changed files per project for incremental code map generation.
 * This singleton maintains a thread-safe record of file changes between API calls.
 *
 * @since 1.0.0
 */
public class ChangedFilesTracker {

    private static ChangedFilesTracker instance;

    // Map: projectKey (URI) -> Set of changed file names
    private final Map<String, Set<String>> changedFilesMap;

    private ChangedFilesTracker() {
        this.changedFilesMap = new ConcurrentHashMap<>();
    }

    public static synchronized ChangedFilesTracker getInstance() {
        if (instance == null) {
            instance = new ChangedFilesTracker();
        }
        return instance;
    }

    // Track a changed file for a given project.
    public void trackFile(String projectKey, String fileName) {
        changedFilesMap
                .computeIfAbsent(projectKey, k -> ConcurrentHashMap.newKeySet())
                .add(fileName);
    }

     // Get and clear the list of changed files for a project.
    public List<String> getAndClearChangedFiles(String projectKey) {
        Set<String> files = changedFilesMap.remove(projectKey);
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(files);
    }

}

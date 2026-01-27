/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
 * Tracks modified (changed or added) files per project for incremental code map generation.
 * This singleton maintains a thread-safe record of file modifications between API calls.
 *
 * @since 1.6.0
 */
public class CodeMapFilesTracker {

    // Map: projectKey (URI) -> Set of modified (changed or added) file relative paths
    private final Map<String, Set<String>> modifiedFilesMap;

    private CodeMapFilesTracker() {
        this.modifiedFilesMap = new ConcurrentHashMap<>();
    }

    private static class Holder {
        private static final CodeMapFilesTracker INSTANCE = new CodeMapFilesTracker();
    }

    /**
     * Returns the singleton instance of ChangedFilesTracker.
     *
     * @return the ChangedFilesTracker instance
     */
    public static CodeMapFilesTracker getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Track a changed file for a given project.
     *
     * @param projectKey   the project identifier
     * @param relativePath the relative path of the changed file from project root
     */
    public void trackFile(String projectKey, String relativePath) {
        modifiedFilesMap
                .computeIfAbsent(projectKey, k -> ConcurrentHashMap.newKeySet())
                .add(relativePath);
    }

    /**
     * Retrieves all tracked modified files for the given project.
     *
     * @param projectKey the project URI key
     * @return list of modified file relative paths, or empty list if none tracked
     */
    public List<String> getModifiedFiles(String projectKey) {
        Set<String> files = modifiedFilesMap.get(projectKey);
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(files);
    }

    /**
     * Clears all tracked modified files for the given project.
     *
     * @param projectKey the project URI key
     */
    public void clearModifiedFiles(String projectKey) {
        modifiedFilesMap.remove(projectKey);
    }

}

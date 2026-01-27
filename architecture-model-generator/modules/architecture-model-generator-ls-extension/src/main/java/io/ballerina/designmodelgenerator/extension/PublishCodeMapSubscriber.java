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

package io.ballerina.designmodelgenerator.extension;

import io.ballerina.artifactsgenerator.codemap.CodeMapFilesTracker;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.eventsync.EventKind;
import org.ballerinalang.langserver.commons.eventsync.spi.EventSubscriber;

import java.nio.file.Path;

/**
 * Tracks changed files for incremental code map generation.
 * When a file is opened or changed, this subscriber records the file name.
 * The tracked files are used when the client calls the codeMap API.
 *
 * @since 1.6.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.eventsync.spi.EventSubscriber")
public class PublishCodeMapSubscriber implements EventSubscriber {

    public static final String NAME = "Publish code map subscriber";
    private static final String FILE_URI = "file";
    private static final String DID_CHANGE = "text/didChange";
    private static final String DID_OPEN = "text/didOpen";

    @Override
    public EventKind eventKind() {
        return EventKind.PROJECT_UPDATE;
    }

    @Override
    public void onEvent(ExtendedLanguageClient client, DocumentServiceContext context,
                        LanguageServerContext serverContext) {
        // Only track files on didChange and didOpen events
        String operationName = context.operation().getName();
        if (!DID_CHANGE.equals(operationName) && !DID_OPEN.equals(operationName)) {
            return;
        }

        // Skip tracking for AI cloned projects and expression editor
        if (!context.fileUri().startsWith(FILE_URI)) {
            return;
        }

        // Get the project key and relative file path
        Path projectPath = context.workspace().projectRoot(context.filePath());
        if (projectPath == null) {
            return;
        }
        String projectKey = projectPath.toUri().toString();
        String relativePath = projectPath.relativize(context.filePath()).toString();

        // Track the changed file
        CodeMapFilesTracker.getInstance().trackFile(projectKey, relativePath);
    }

    @Override
    public String getName() {
        return NAME;
    }
}

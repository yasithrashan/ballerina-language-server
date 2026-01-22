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

package io.ballerina.designmodelgenerator.extension;

import com.google.gson.JsonObject;
import io.ballerina.artifactsgenerator.codemap.ChangedFilesTracker;
import io.ballerina.designmodelgenerator.extension.request.CodeMapRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.ballerinalang.langserver.LSContextOperation;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.ballerinalang.langserver.commons.LSOperation;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.contexts.ContextBuilder;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test cases for PublishCodeMapSubscriber and codeMapChanges API.
 *
 * @since 1.0.0
 */
public class PublishCodeMapSubscriberTest extends AbstractLSTest {

    private final PublishCodeMapSubscriber publishCodeMapSubscriber = new PublishCodeMapSubscriber();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath(testConfig.source());
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        // Create document service context
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.TXT_DID_CHANGE,
                languageServer.getServerContext()
        );

        // Simulate didChange notification
        VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier();
        List<TextDocumentContentChangeEvent> changeEvents =
                List.of(new TextDocumentContentChangeEvent(getText(sourcePath)));

        try {
            workspaceManager.didChange(filePath,
                    new DidChangeTextDocumentParams(versionedTextDocumentIdentifier, changeEvents));
        } catch (WorkspaceDocumentException e) {
            Assert.fail("Error while sending didChange notification", e);
        }

        // Invoke the subscriber to track the changed file
        publishCodeMapSubscriber.onEvent(
                null, // client is not used by this subscriber
                documentServiceContext,
                languageServer.getServerContext()
        );

        // Call the codeMapChanges API and verify response
        Path projectPath = workspaceManager.projectRoot(filePath);
        CodeMapRequest request = new CodeMapRequest(projectPath.toString());
        JsonObject codeMapResponse = getResponse(request, "designModelService/codeMapChanges");
        JsonObject files = codeMapResponse.getAsJsonObject("files");

        if (!files.equals(testConfig.output())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.source(), files);
//            updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(files, testConfig.output());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Test
    public void testNoChangesTracked() throws IOException {
        // Clear any previously tracked files
        String sourcePath = getSourcePath("project");
        Path projectPath = Path.of(sourcePath);
        String projectKey = projectPath.toUri().toString();
        ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);

        // Call codeMapChanges without tracking any files - should return empty
        CodeMapRequest request = new CodeMapRequest(sourcePath);
        JsonObject codeMapResponse = getResponse(request, "designModelService/codeMapChanges");
        JsonObject files = codeMapResponse.getAsJsonObject("files");

        Assert.assertTrue(files.entrySet().isEmpty(),
                "Expected empty files response when no changes are tracked");
    }

    @Test
    public void testSubscriberEventKind() {
        Assert.assertEquals(publishCodeMapSubscriber.eventKind(),
                org.ballerinalang.langserver.commons.eventsync.EventKind.PROJECT_UPDATE,
                "Subscriber should respond to PROJECT_UPDATE events");
    }

    @Test
    public void testSubscriberName() {
        Assert.assertEquals(publishCodeMapSubscriber.getName(),
                PublishCodeMapSubscriber.NAME,
                "Subscriber name should match");
    }

    @Test
    public void testSkipsAiUri() throws IOException {
        // Test that AI URI files are skipped
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);

        // Create a mock context with AI URI
        DocumentServiceContext mockContext = Mockito.mock(DocumentServiceContext.class);
        LSOperation mockOperation = Mockito.mock(LSOperation.class);
        Mockito.when(mockOperation.getName()).thenReturn("text/didChange");
        Mockito.when(mockContext.operation()).thenReturn(mockOperation);
        Mockito.when(mockContext.fileUri()).thenReturn("ai://test/main.bal");

        // This should not track the file due to AI URI (returns early before accessing workspace)
        publishCodeMapSubscriber.onEvent(null, mockContext, languageServer.getServerContext());

        // Verify nothing was tracked
        List<String> trackedFiles = ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);
        Assert.assertTrue(trackedFiles.isEmpty(), "AI URI files should not be tracked");
    }

    @Test
    public void testSkipsExprUri() throws IOException {
        // Test that expr URI files are skipped
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);

        // Create a mock context with expr URI
        DocumentServiceContext mockContext = Mockito.mock(DocumentServiceContext.class);
        LSOperation mockOperation = Mockito.mock(LSOperation.class);
        Mockito.when(mockOperation.getName()).thenReturn("text/didChange");
        Mockito.when(mockContext.operation()).thenReturn(mockOperation);
        Mockito.when(mockContext.fileUri()).thenReturn("expr://test/main.bal");

        // This should not track the file due to expr URI (returns early before accessing workspace)
        publishCodeMapSubscriber.onEvent(null, mockContext, languageServer.getServerContext());

        // Verify nothing was tracked
        List<String> trackedFiles = ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);
        Assert.assertTrue(trackedFiles.isEmpty(), "Expr URI files should not be tracked");
    }

    @Test
    public void testSkipsNonDidChangeOperations() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        // Create a context with a different operation (not didChange)
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.TXT_DID_OPEN,
                languageServer.getServerContext()
        );

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);

        // This should not track the file due to non-didChange operation
        publishCodeMapSubscriber.onEvent(null, documentServiceContext, languageServer.getServerContext());

        // Verify nothing was tracked
        List<String> trackedFiles = ChangedFilesTracker.getInstance().getAndClearChangedFiles(projectKey);
        Assert.assertTrue(trackedFiles.isEmpty(), "Non-didChange operations should not track files");
    }

    @Override
    protected String getResourceDir() {
        return "codemap_changes";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return PublishCodeMapSubscriberTest.class;
    }

    @Override
    protected String getServiceName() {
        return "designModelService";
    }

    @Override
    protected String getApiName() {
        return "codeMapChanges";
    }

    public record TestConfig(String description, String source, JsonObject output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}

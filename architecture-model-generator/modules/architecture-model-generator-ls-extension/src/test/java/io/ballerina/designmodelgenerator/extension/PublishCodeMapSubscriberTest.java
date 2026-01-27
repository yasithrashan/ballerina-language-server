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

import com.google.gson.JsonObject;
import io.ballerina.artifactsgenerator.codemap.CodeMapFilesTracker;
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
 * Test cases for PublishCodeMapSubscriber and codeMap API with changesOnly mode.
 *
 * @since 1.6.0
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

        // Call the codeMap API with changesOnly=true and verify response
        Path projectPath = workspaceManager.projectRoot(filePath);
        CodeMapRequest request = new CodeMapRequest(projectPath.toString(), true);
        JsonObject codeMapResponse = getResponse(request, "designModelService/codemap");
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
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Call codeMap with changesOnly=true without tracking any files - should return empty
        CodeMapRequest request = new CodeMapRequest(sourcePath, true);
        JsonObject codeMapResponse = getResponse(request, "designModelService/codemap");
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
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Create a mock context with AI URI
        DocumentServiceContext mockContext = Mockito.mock(DocumentServiceContext.class);
        LSOperation mockOperation = Mockito.mock(LSOperation.class);
        Mockito.when(mockOperation.getName()).thenReturn("text/didChange");
        Mockito.when(mockContext.operation()).thenReturn(mockOperation);
        Mockito.when(mockContext.fileUri()).thenReturn("ai://test/main.bal");

        // This should not track the file due to AI URI (returns early before accessing workspace)
        publishCodeMapSubscriber.onEvent(null, mockContext, languageServer.getServerContext());

        // Verify nothing was tracked
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
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
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Create a mock context with expr URI
        DocumentServiceContext mockContext = Mockito.mock(DocumentServiceContext.class);
        LSOperation mockOperation = Mockito.mock(LSOperation.class);
        Mockito.when(mockOperation.getName()).thenReturn("text/didChange");
        Mockito.when(mockContext.operation()).thenReturn(mockOperation);
        Mockito.when(mockContext.fileUri()).thenReturn("expr://test/main.bal");

        // This should not track the file due to expr URI (returns early before accessing workspace)
        publishCodeMapSubscriber.onEvent(null, mockContext, languageServer.getServerContext());

        // Verify nothing was tracked
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
        Assert.assertTrue(trackedFiles.isEmpty(), "Expr URI files should not be tracked");
    }

    @Test
    public void testSkipsNonTrackedOperations() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        // Create a context with a different operation (not didChange or didOpen)
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.TXT_HOVER,
                languageServer.getServerContext()
        );

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // This should not track the file due to non-tracked operation
        publishCodeMapSubscriber.onEvent(null, documentServiceContext, languageServer.getServerContext());

        // Verify nothing was tracked
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
        Assert.assertTrue(trackedFiles.isEmpty(), "Non-tracked operations should not track files");
    }

    @Test
    public void testTracksDidOpenEvents() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Create a context with didOpen operation
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.TXT_DID_OPEN,
                languageServer.getServerContext()
        );

        // This should track the file due to didOpen operation
        publishCodeMapSubscriber.onEvent(null, documentServiceContext, languageServer.getServerContext());

        // Verify file was tracked
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
        Assert.assertEquals(trackedFiles.size(), 1, "didOpen should track the file");
        Assert.assertTrue(trackedFiles.contains("main.bal"), "main.bal should be tracked");

        // Cleanup
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);
    }

    @Test
    public void testTracksBothDidChangeAndDidOpenEvents() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();

        // Get paths for two different files in the same project
        String sourcePath1 = getSourcePath("project/main.bal");
        String sourcePath2 = getSourcePath("project/service.bal");
        Path filePath1 = Path.of(sourcePath1);
        Path filePath2 = Path.of(sourcePath2);

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath1);
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Trigger didOpen event for first file (simulating new file opened)
        String fileUri1 = filePath1.toAbsolutePath().normalize().toUri().toString();
        DocumentServiceContext openContext = ContextBuilder.buildDocumentServiceContext(
                fileUri1, workspaceManager, LSContextOperation.TXT_DID_OPEN, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, openContext, languageServer.getServerContext());

        // Trigger didChange event for second file (simulating file modification)
        String fileUri2 = filePath2.toAbsolutePath().normalize().toUri().toString();
        DocumentServiceContext changeContext = ContextBuilder.buildDocumentServiceContext(
                fileUri2, workspaceManager, LSContextOperation.TXT_DID_CHANGE, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, changeContext, languageServer.getServerContext());

        // Verify both files are tracked
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
        Assert.assertEquals(trackedFiles.size(), 2, "Both didOpen and didChange should track files");
        Assert.assertTrue(trackedFiles.contains("main.bal"), "main.bal (opened) should be tracked");
        Assert.assertTrue(trackedFiles.contains("service.bal"), "service.bal (changed) should be tracked");

        // Cleanup
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);
    }

    @Test
    public void testMultipleFileAccumulation() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();

        // Get paths for two different files in the same project
        String sourcePath1 = getSourcePath("project/main.bal");
        String sourcePath2 = getSourcePath("project/service.bal");
        Path filePath1 = Path.of(sourcePath1);
        Path filePath2 = Path.of(sourcePath2);

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath1);
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Trigger onEvent for first file
        String fileUri1 = filePath1.toAbsolutePath().normalize().toUri().toString();
        DocumentServiceContext context1 = ContextBuilder.buildDocumentServiceContext(
                fileUri1, workspaceManager, LSContextOperation.TXT_DID_CHANGE, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, context1, languageServer.getServerContext());

        // Trigger onEvent for second file
        String fileUri2 = filePath2.toAbsolutePath().normalize().toUri().toString();
        DocumentServiceContext context2 = ContextBuilder.buildDocumentServiceContext(
                fileUri2, workspaceManager, LSContextOperation.TXT_DID_CHANGE, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, context2, languageServer.getServerContext());

        // Verify both files are tracked
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
        Assert.assertEquals(trackedFiles.size(), 2, "Both files should be tracked");
        Assert.assertTrue(trackedFiles.contains("main.bal"), "main.bal should be tracked");
        Assert.assertTrue(trackedFiles.contains("service.bal"), "service.bal should be tracked");

        // Cleanup
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);
    }

    @Test
    public void testConsecutiveEventsForSameFile() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Trigger onEvent multiple times for the same file
        DocumentServiceContext context = ContextBuilder.buildDocumentServiceContext(
                fileUri, workspaceManager, LSContextOperation.TXT_DID_CHANGE, languageServer.getServerContext());

        publishCodeMapSubscriber.onEvent(null, context, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, context, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, context, languageServer.getServerContext());

        // Verify the file is tracked only once (no duplicates)
        List<String> trackedFiles = CodeMapFilesTracker.getInstance().getModifiedFiles(projectKey);
        Assert.assertEquals(trackedFiles.size(), 1, "File should be tracked only once despite multiple events");
        Assert.assertTrue(trackedFiles.contains("main.bal"), "main.bal should be tracked");

        // Cleanup
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);
    }

    @Test
    public void testSameFileNameInDifferentModulesTrackedSeparately() {
        // Negative test: changing root types.bal should NOT track modules/mod1/types.bal
        String projectKey = "file:///test/project/";
        CodeMapFilesTracker tracker = CodeMapFilesTracker.getInstance();

        // Clear tracker first
        tracker.clearModifiedFiles(projectKey);

        // Track only the root module file
        tracker.trackFile(projectKey, "types.bal");

        // Verify only root file is tracked, submodule file should NOT be present
        List<String> trackedFiles = tracker.getModifiedFiles(projectKey);
        Assert.assertEquals(trackedFiles.size(), 1,
                "Only one file should be tracked");
        Assert.assertTrue(trackedFiles.contains("types.bal"),
                "Root types.bal should be tracked");
        Assert.assertFalse(trackedFiles.contains("modules/mod1/types.bal"),
                "Submodule types.bal should NOT be tracked when only root file changed");

        // Cleanup
        tracker.clearModifiedFiles(projectKey);
    }

    @Test
    public void testStateClearingAfterRetrieval() throws IOException {
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("project/main.bal");
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        // Clear tracker first
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker.getInstance().clearModifiedFiles(projectKey);

        // Track a file
        DocumentServiceContext context = ContextBuilder.buildDocumentServiceContext(
                fileUri, workspaceManager, LSContextOperation.TXT_DID_CHANGE, languageServer.getServerContext());
        publishCodeMapSubscriber.onEvent(null, context, languageServer.getServerContext());

        // Call codeMap API with changesOnly=true (this should consume the tracked changes)
        CodeMapRequest request = new CodeMapRequest(projectPath.toString(), true);
        JsonObject codeMapResponse = getResponse(request, "designModelService/codemap");
        JsonObject files = codeMapResponse.getAsJsonObject("files");
        Assert.assertFalse(files.entrySet().isEmpty(), "First call should return tracked changes");

        // Call codeMap API again - should return empty since changes were consumed
        JsonObject secondResponse = getResponse(request, "designModelService/codemap");
        JsonObject secondFiles = secondResponse.getAsJsonObject("files");
        Assert.assertTrue(secondFiles.entrySet().isEmpty(),
                "Second call should return empty as changes were consumed by first call");
    }

    @Test
    public void testProjectIsolation() {
        // Test that changes in one project do not leak into another project
        String projectKeyA = "file:///test/projectA/";
        String projectKeyB = "file:///test/projectB/";
        CodeMapFilesTracker tracker = CodeMapFilesTracker.getInstance();

        // Clear both trackers
        tracker.clearModifiedFiles(projectKeyA);
        tracker.clearModifiedFiles(projectKeyB);

        // Track file in Project A only
        tracker.trackFile(projectKeyA, "main.bal");
        tracker.trackFile(projectKeyA, "service.bal");

        // Verify Project A has tracked files
        List<String> trackedFilesA = tracker.getModifiedFiles(projectKeyA);
        Assert.assertEquals(trackedFilesA.size(), 2, "Project A should have 2 tracked files");

        // Verify Project B has no tracked files (isolation)
        List<String> trackedFilesB = tracker.getModifiedFiles(projectKeyB);
        Assert.assertTrue(trackedFilesB.isEmpty(),
                "Project B should have no tracked files - changes should not leak between projects");

        // Now track a file in Project B
        tracker.trackFile(projectKeyB, "utils.bal");

        // Verify Project A still has its original files (unchanged)
        trackedFilesA = tracker.getModifiedFiles(projectKeyA);
        Assert.assertEquals(trackedFilesA.size(), 2, "Project A should still have 2 tracked files");
        Assert.assertFalse(trackedFilesA.contains("utils.bal"),
                "Project A should not contain Project B's file");

        // Verify Project B has only its file
        trackedFilesB = tracker.getModifiedFiles(projectKeyB);
        Assert.assertEquals(trackedFilesB.size(), 1, "Project B should have 1 tracked file");
        Assert.assertTrue(trackedFilesB.contains("utils.bal"), "Project B should contain utils.bal");

        // Cleanup
        tracker.clearModifiedFiles(projectKeyA);
        tracker.clearModifiedFiles(projectKeyB);
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
        return "codemap";
    }

    public record TestConfig(String description, String source, JsonObject output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}

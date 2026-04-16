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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates code map from Ballerina projects by extracting artifacts from source files.
 *
 * @since 1.6.0
 */
public class CodeMapGenerator {

    /**
     * Generates a code map for all files in the given project.
     *
     * @param project          the Ballerina project
     * @param workspaceManager the workspace manager to obtain semantic models
     * @return a map of relative file paths to their code map files
     */
    public static Map<String, CodeMapFile> generateCodeMap(Project project, WorkspaceManager workspaceManager) {
        return generateCodeMap(project, workspaceManager, null);
    }

    /**
     * Generates a code map for specific files in the given project. If {@code fileNames} is {@code null},
     * all files in the project are processed.
     *
     * @param project          the Ballerina project
     * @param workspaceManager the workspace manager to obtain semantic models
     * @param fileNames        the list of file names to process, or {@code null} to process all files
     * @return a map of relative file paths to their code map files
     */
    public static Map<String, CodeMapFile> generateCodeMap(Project project, WorkspaceManager workspaceManager,
                                                           List<String> fileNames) {
        Package currentPackage = project.currentPackage();
        Map<String, CodeMapFile> codeMapFiles = new LinkedHashMap<>();
        String projectPath = project.sourceRoot().toAbsolutePath().toString();
        Set<String> targetFiles = fileNames != null ? new HashSet<>(fileNames) : null;

        // Sort modules alphabetically for consistent order
        var sortedModules = currentPackage.moduleIds()
                .stream()
                .sorted(Comparator.comparing(moduleId -> {
                    Module m = currentPackage.module(moduleId);
                    return m.isDefaultModule() ? "" : m.moduleName().moduleNamePart();
                }))
                .collect(Collectors.toList());

        for (var moduleId : sortedModules) {
            Module module = currentPackage.module(moduleId);
            ModuleInfo moduleInfo = ModuleInfo.from(module.descriptor());

            // Sort documents alphabetically for consistent order
            var sortedDocs = module.documentIds()
                    .stream()
                    .sorted(Comparator.comparing(docId -> module.document(docId).name()))
                    .collect(Collectors.toList());

            for (var documentId : sortedDocs) {
                Document document = module.document(documentId);
                String fileName = document.name();
                String relativeFilePath = getRelativeFilePath(module, fileName);

                // Ignore the file if it is not in the targeted list.
                if (targetFiles != null && !targetFiles.contains(relativeFilePath)) {
                    continue;
                }

                Path filePath = getDocumentPath(project, module, fileName);
                Optional<SemanticModel> semanticModelOpt = workspaceManager.semanticModel(filePath);
                if (semanticModelOpt.isEmpty()) {
                    continue;
                }

                SyntaxTree syntaxTree = document.syntaxTree();
                List<CodeMapArtifact> artifacts = collectArtifactsFromSyntaxTree(projectPath, syntaxTree,
                        semanticModelOpt.get(), moduleInfo);

                CodeMapFile codeMapFile = new CodeMapFile(artifacts);
                codeMapFiles.put(relativeFilePath, codeMapFile);
            }
        }

        return codeMapFiles;
    }

    private static List<CodeMapArtifact> collectArtifactsFromSyntaxTree(String projectPath, SyntaxTree syntaxTree,
                                                                        SemanticModel semanticModel,
                                                                        ModuleInfo moduleInfo) {
        List<CodeMapArtifact> artifacts = new ArrayList<>();

        // Handle syntax errors first - create artifacts for each diagnostic
        if (syntaxTree.hasDiagnostics()) {
            List<CodeMapArtifact> syntaxErrorArtifacts = createSyntaxErrorArtifacts(
                    syntaxTree.diagnostics(), syntaxTree);
            artifacts.addAll(syntaxErrorArtifacts);
        }

        if (!syntaxTree.containsModulePart()) {
            return artifacts;
        }

        ModulePartNode rootNode = syntaxTree.rootNode();
        CodeMapNodeTransformer codeMapNodeTransformer = new CodeMapNodeTransformer(projectPath, semanticModel,
                moduleInfo);

        try {
            // Process imports - filter out malformed ones to avoid transformation errors
            rootNode.imports().stream()
                    .filter(importNode -> !hasErrorInNode(importNode))
                    .map(importNode -> importNode.apply(codeMapNodeTransformer))
                    .flatMap(Optional::stream)
                    .forEach(artifacts::add);

            // Process other members (functions, services, types, etc.) - filter out malformed ones
            rootNode.members().stream()
                    .filter(member -> !hasErrorInNode(member))
                    .map(member -> member.apply(codeMapNodeTransformer))
                    .flatMap(Optional::stream)
                    .forEach(artifacts::add);
        } catch (RuntimeException e) {
            // Fallback: if processing fails due to severe syntax errors, create a general error artifact
            CodeMapArtifact errorArtifact = createGeneralSyntaxErrorArtifact(e.getMessage());
            artifacts.add(errorArtifact);
        } catch (Exception e) {
            // Handle unexpected checked exceptions
            CodeMapArtifact errorArtifact = createGeneralSyntaxErrorArtifact("Unexpected error: " + e.getMessage());
            artifacts.add(errorArtifact);
        }

        return artifacts;
    }


    private static List<CodeMapArtifact> createSyntaxErrorArtifacts(Iterable<Diagnostic> diagnostics,
                                                                       SyntaxTree syntaxTree) {
        List<CodeMapArtifact> syntaxErrorArtifacts = new ArrayList<>();

        for (Diagnostic diagnostic : diagnostics) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("diagnosticMessage", diagnostic.message());
            properties.put("severity", diagnostic.diagnosticInfo().severity().toString());
            properties.put("code", diagnostic.diagnosticInfo().code());

            // Extract raw source code for the error lines if syntax tree is available
            if (syntaxTree != null) {
                String rawCode = extractRawCodeFromDiagnostic(diagnostic, syntaxTree);
                if (rawCode != null && !rawCode.trim().isEmpty()) {
                    properties.put("rawCode", rawCode);
                }
            }

            CodeMapArtifact syntaxErrorArtifact = new CodeMapArtifact(
                "Syntax Error",
                "SYNTAX_ERROR",
                CodeMapArtifact.toRange(diagnostic.location().lineRange()),
                properties,
                Collections.emptyList()
            );
            syntaxErrorArtifacts.add(syntaxErrorArtifact);
        }

        return syntaxErrorArtifacts;
    }

    private static String extractRawCodeFromDiagnostic(Diagnostic diagnostic, SyntaxTree syntaxTree) {
        try {
            String sourceText = syntaxTree.toSourceCode();
            if (sourceText == null || sourceText.isEmpty()) {
                return null;
            }
            String[] lines = sourceText.split("\\r?\\n");

            // Get diagnostic line range (0-based indexing from compiler API)
            int startLine = diagnostic.location().lineRange().startLine().line();
            int endLine = diagnostic.location().lineRange().endLine().line();

            // Validate line bounds to prevent array access errors
            if (startLine < 0 || startLine >= lines.length || endLine < startLine) {
                return null;
            }

            // Ensure end line is within bounds
            int safeEndLine = Math.min(endLine, lines.length - 1);

            // Extract the problematic code lines for error context
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = startLine; i <= safeEndLine; i++) {
                if (i > startLine) {
                    codeBuilder.append("\n");
                }
                codeBuilder.append(lines[i]);
            }

            return codeBuilder.toString().trim();
        } catch (IndexOutOfBoundsException e) {
            // Return null if line extraction fails due to invalid indices
            return null;
        } catch (RuntimeException e) {
            // Handle other runtime issues during source code extraction
            return null;
        }
    }

    private static CodeMapArtifact createGeneralSyntaxErrorArtifact(String errorMessage) {
        return new CodeMapArtifact(
            "Parsing Error",
            "SYNTAX_ERROR",
            null, // No specific range available
            Map.of("errorMessage", "Failed to parse file: " + errorMessage),
            Collections.emptyList()
        );
    }

    private static boolean hasErrorInNode(io.ballerina.compiler.syntax.tree.Node node) {
        if (node == null) {
            return true;
        }

        // Primary check: node has compiler diagnostics indicating errors
        if (node.hasDiagnostics()) {
            return true;
        }

        // Validate source code extraction - failure indicates malformed node
        try {
            String sourceText = node.toSourceCode();
            if (sourceText == null || sourceText.trim().isEmpty()) {
                return true;
            }
            // Check for error markers that indicate parsing issues
            return sourceText.contains("MISSING") || sourceText.contains("[error]");
        } catch (RuntimeException e) {
            // Source code extraction failed - treat as error node
            return true;
        }
    }

    private static String getRelativeFilePath(Module module, String fileName) {
        if (module.isDefaultModule()) {
            return fileName;
        }
        String moduleName = module.moduleName().moduleNamePart();
        return "modules" + File.separator + moduleName + File.separator + fileName;
    }

    private static Path getDocumentPath(Project project, Module module, String fileName) {
        Path sourceRoot = project.sourceRoot();
        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return sourceRoot;
        }
        if (module.isDefaultModule()) {
            return sourceRoot.resolve(fileName);
        }
        return sourceRoot.resolve("modules").resolve(module.moduleName().moduleNamePart()).resolve(fileName);
    }

    /**
     * Processes incremental changes and returns a response map containing modified files.
     *
     * @param project the Ballerina project
     * @param workspaceManager the workspace manager
     * @param projectPath the project path
     * @return response map with modifiedFiles
     */
    public static Map<String, Object> processIncrementalChanges(Project project, WorkspaceManager workspaceManager,
                                                                Path projectPath) {
        String projectKey = projectPath.toUri().toString();
        CodeMapFilesTracker tracker = CodeMapFilesTracker.getInstance();

        // Get tracked changes
        List<String> modifiedFiles = tracker.getModifiedFiles(projectKey);

        // No changes detected
        if (modifiedFiles.isEmpty()) {
            return java.util.Collections.emptyMap();
        }

        // Generate codemap for modified files only
        Map<String, CodeMapFile> codeMapFiles = generateCodeMap(project, workspaceManager, modifiedFiles);

        // Build incremental response structure
        Map<String, Map<String, Object>> modifiedFilesData = new LinkedHashMap<>();
        for (Map.Entry<String, CodeMapFile> entry : codeMapFiles.entrySet()) {
            String filePath = entry.getKey();
            CodeMapFile codeMapFile = entry.getValue();
            String fileMarkdown = CodeMapMarkdownGenerator.generateFileMarkdown(filePath, codeMapFile);

            Map<String, Object> fileData = new HashMap<>();
            fileData.put("markdown", fileMarkdown);
            modifiedFilesData.put(filePath, fileData);
        }

        // Build response with changes structure
        Map<String, Object> changesResponse = new LinkedHashMap<>();
        changesResponse.put("modifiedFiles", modifiedFilesData);
        changesResponse.put("deletedFiles", Collections.emptyList());

        // Clear tracker after successful processing
        tracker.clearAllFiles(projectKey);

        return changesResponse;
    }

    /**
     * Processes full project codemap and returns consolidated markdown content.
     *
     * @param project the Ballerina project
     * @param workspaceManager the workspace manager
     * @return consolidated project markdown content
     */
    public static String processFullProjectCodeMap(Project project, WorkspaceManager workspaceManager) {
        // Generate full project codemap
        Map<String, CodeMapFile> codeMapFiles = generateCodeMap(project, workspaceManager);

        // Extract project name
        String projectName = project.currentPackage().packageName().value();

        // Convert to consolidated markdown
        return CodeMapMarkdownGenerator.generateMarkdown(codeMapFiles, projectName);
    }

    /**
     * Generates a code map for all packages in a workspace.
     *
     * @param project the Ballerina workspace project
     * @param workspaceManager the workspace manager to obtain semantic models
     * @return a map of package names to their code map files
     */
    public static Map<String, Map<String, CodeMapFile>> generateWorkspaceCodeMap(Project project,
                                                                                  WorkspaceManager workspaceManager) {
        return generateWorkspaceCodeMap(project, workspaceManager, null);
    }

    /**
     * Generates a code map for specific files in all packages of a workspace.
     *
     * @param project the Ballerina workspace project
     * @param workspaceManager the workspace manager to obtain semantic models
     * @param fileNames the list of file names to process, or {@code null} to process all files
     * @return a map of package names to their code map files
     */
    public static Map<String, Map<String, CodeMapFile>> generateWorkspaceCodeMap(Project project,
                                                                                  WorkspaceManager workspaceManager,
                                                                                  List<String> fileNames) {
        Map<String, Map<String, CodeMapFile>> workspaceCodeMap = new LinkedHashMap<>();
        BallerinaCompilerApi compilerApi = BallerinaCompilerApi.getInstance();

        // Check if this is a workspace project
        if (!compilerApi.isWorkspaceProject(project)) {
            // If not a workspace, just include the current package
            String packageName = project.currentPackage().packageName().value();
            workspaceCodeMap.put(packageName, generateCodeMap(project, workspaceManager, fileNames));
            return workspaceCodeMap;
        }

        // Get all workspace packages in topological order
        List<Project> workspaceProjects = compilerApi.getWorkspaceProjectsInOrder(project);

        for (Project packageProject : workspaceProjects) {
            String packageName = packageProject.currentPackage().packageName().value();
            Map<String, CodeMapFile> packageCodeMap = generateCodeMap(packageProject, workspaceManager, fileNames);
            workspaceCodeMap.put(packageName, packageCodeMap);
        }

        return workspaceCodeMap;
    }

    /**
     * Processes incremental changes for workspace and returns a response map containing modified files
     * for all packages.
     *
     * @param project the Ballerina workspace project
     * @param workspaceManager the workspace manager
     * @param projectPath the project path
     * @return response map with modifiedFiles for all packages
     */
    public static Map<String, Object> processWorkspaceIncrementalChanges(Project project,
                                                                         WorkspaceManager workspaceManager,
                                                                         Path projectPath) {
        BallerinaCompilerApi compilerApi = BallerinaCompilerApi.getInstance();

        // Check if this is a workspace project
        if (!compilerApi.isWorkspaceProject(project)) {
            // If not a workspace, use single package incremental processing
            return processIncrementalChanges(project, workspaceManager, projectPath);
        }

        Map<String, Object> workspaceChangesResponse = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, Object>>> workspaceModifiedFiles = new LinkedHashMap<>();

        // Get all workspace packages
        List<Project> workspaceProjects = compilerApi.getWorkspaceProjectsInOrder(project);

        for (Project packageProject : workspaceProjects) {
            String packageName = packageProject.currentPackage().packageName().value();
            Path packagePath = packageProject.sourceRoot();

            // Process incremental changes for this package
            Map<String, Object> packageChanges = processIncrementalChanges(packageProject, workspaceManager,
                    packagePath);

            if (!packageChanges.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> modifiedFiles =
                    (Map<String, Map<String, Object>>) packageChanges.get("modifiedFiles");

                if (modifiedFiles != null && !modifiedFiles.isEmpty()) {
                    workspaceModifiedFiles.put(packageName, modifiedFiles);
                }
            }
        }

        // Only add to response if there are changes
        if (!workspaceModifiedFiles.isEmpty()) {
            workspaceChangesResponse.put("modifiedFiles", workspaceModifiedFiles);
        }
        workspaceChangesResponse.put("deletedFiles", Collections.emptyMap());

        return workspaceChangesResponse;
    }

    /**
     * Processes full workspace codemap and returns consolidated markdown content for all packages.
     *
     * @param project the Ballerina workspace project
     * @param workspaceManager the workspace manager
     * @return consolidated workspace markdown content
     */
    public static String processFullWorkspaceCodeMap(Project project, WorkspaceManager workspaceManager) {
        // Generate full workspace codemap
        Map<String, Map<String, CodeMapFile>> workspaceCodeMap = generateWorkspaceCodeMap(project, workspaceManager);

        // Extract workspace name
        Path sourceRoot = project.sourceRoot();
        String workspaceName = "Unknown Workspace";
        if (sourceRoot != null) {
            Path fileName = sourceRoot.getFileName();
            if (fileName != null) {
                workspaceName = fileName.toString();
            }
        }

        // Convert to consolidated workspace markdown
        return CodeMapMarkdownGenerator.generateWorkspaceMarkdown(workspaceCodeMap, workspaceName);
    }
}

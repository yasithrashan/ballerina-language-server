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
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

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

        var sortedModules = currentPackage.moduleIds()
                .stream()
                .sorted(Comparator.comparing(moduleId -> {
                    Module m = currentPackage.module(moduleId);
                    return m.isDefaultModule() ? "" : m.moduleName().moduleNamePart();
                }))
                .collect(Collectors.toList());

        for (ModuleId moduleId : sortedModules) {
            Module module = currentPackage.module(moduleId);
            ModuleInfo moduleInfo = ModuleInfo.from(module.descriptor());

            List<DocumentId> sortedDocs = module.documentIds()
                    .stream()
                    .sorted(Comparator.comparing(docId -> module.document(docId).name()))
                    .collect(Collectors.toList());

            for (DocumentId documentId : sortedDocs) {
                Document document = module.document(documentId);
                String fileName = document.name();
                String relativeFilePath = getRelativeFilePath(module, fileName);

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

    // Collects code artifacts from syntax tree with error handling
    private static List<CodeMapArtifact> collectArtifactsFromSyntaxTree(String projectPath, SyntaxTree syntaxTree,
                                                                        SemanticModel semanticModel,
                                                                        ModuleInfo moduleInfo) {
        List<CodeMapArtifact> artifacts = new ArrayList<>();

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

        // Process imports individually with per-node error handling
        rootNode.imports().forEach(importNode -> addArtifactSafely(importNode, codeMapNodeTransformer, artifacts));

        // Process members individually with per-node error handling
        rootNode.members().forEach(member -> addArtifactSafely(member, codeMapNodeTransformer, artifacts));
        return artifacts;
    }


    // Creates artifacts for syntax errors found in diagnostics
    private static List<CodeMapArtifact> createSyntaxErrorArtifacts(Iterable<Diagnostic> diagnostics,
                                                                    SyntaxTree syntaxTree) {
        List<CodeMapArtifact> syntaxErrorArtifacts = new ArrayList<>();

        for (Diagnostic diagnostic : diagnostics) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("diagnosticMessage", diagnostic.message());
            properties.put("severity", diagnostic.diagnosticInfo().severity().toString());
            properties.put("code", diagnostic.diagnosticInfo().code());

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

    // Extracts raw source code from diagnostic location
    private static String extractRawCodeFromDiagnostic(Diagnostic diagnostic, SyntaxTree syntaxTree) {
        try {
            String sourceText = syntaxTree.toSourceCode();
            if (sourceText == null || sourceText.isEmpty()) {
                return null;
            }
            String[] lines = sourceText.split("\\r?\\n");

            int startLine = diagnostic.location().lineRange().startLine().line();
            int endLine = diagnostic.location().lineRange().endLine().line();

            if (startLine < 0 || startLine >= lines.length || endLine < startLine) {
                return null;
            }

            int safeEndLine = Math.min(endLine, lines.length - 1);

            StringBuilder codeBuilder = new StringBuilder();
            for (int i = startLine; i <= safeEndLine; i++) {
                if (i > startLine) {
                    codeBuilder.append("\n");
                }
                codeBuilder.append(lines[i]);
            }

            return codeBuilder.toString().trim();
        } catch (IndexOutOfBoundsException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // Creates a general syntax error artifact when specific error details are unavailable
    private static CodeMapArtifact createGeneralSyntaxErrorArtifact(String errorMessage) {
        return new CodeMapArtifact(
                "Parsing Error",
                "SYNTAX_ERROR",
                null,
                Map.of("errorMessage", "Failed to parse file: " + errorMessage),
                Collections.emptyList()
        );
    }

    // Checks if a syntax node contains errors or missing tokens
    private static boolean hasErrorInNode(io.ballerina.compiler.syntax.tree.Node node) {
        if (node == null) {
            return true;
        }

        if (node.hasDiagnostics()) {
            return true;
        }

        try {
            String sourceText = node.toSourceCode();
            if (sourceText == null || sourceText.trim().isEmpty()) {
                return true;
            }
            return sourceText.contains("MISSING") || sourceText.contains("[error]");
        } catch (RuntimeException e) {
            return true;
        }
    }

    // Gets relative file path considering module structure
    private static String getRelativeFilePath(Module module, String fileName) {
        if (module.isDefaultModule()) {
            return fileName;
        }
        String moduleName = module.moduleName().moduleNamePart();
        return "modules/" + moduleName + "/" + fileName;
    }

    // Gets full document path for a file within a module
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
     * Processes full project codemap and returns consolidated markdown content.
     *
     * @param project          the Ballerina project
     * @param workspaceManager the workspace manager
     * @return consolidated project markdown content
     */
    public static String processFullProjectCodeMap(Project project, WorkspaceManager workspaceManager) {
        Map<String, CodeMapFile> codeMapFiles = generateCodeMap(project, workspaceManager);

        String projectName = project.currentPackage().packageName().value();

        return CodeMapMarkdownGenerator.generateMarkdown(codeMapFiles, projectName);
    }

    /**
     * Generates a code map for all packages in a workspace.
     *
     * @param project          the Ballerina workspace project
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
     * @param project          the Ballerina workspace project
     * @param workspaceManager the workspace manager to obtain semantic models
     * @param fileNames        the list of file names to process, or {@code null} to process all files
     * @return a map of package names to their code map files
     */
    public static Map<String, Map<String, CodeMapFile>> generateWorkspaceCodeMap(Project project,
                                                                                 WorkspaceManager workspaceManager,
                                                                                 List<String> fileNames) {
        Map<String, Map<String, CodeMapFile>> workspaceCodeMap = new LinkedHashMap<>();
        BallerinaCompilerApi compilerApi = BallerinaCompilerApi.getInstance();

        if (!compilerApi.isWorkspaceProject(project)) {
            String packageName = project.currentPackage().packageName().value();
            workspaceCodeMap.put(packageName, generateCodeMap(project, workspaceManager, fileNames));
            return workspaceCodeMap;
        }

        List<Project> workspaceProjects = compilerApi.getWorkspaceProjectsInOrder(project);

        for (Project packageProject : workspaceProjects) {
            String packageName = packageProject.currentPackage().packageName().value();
            Map<String, CodeMapFile> packageCodeMap = generateCodeMap(packageProject, workspaceManager, fileNames);
            workspaceCodeMap.put(packageName, packageCodeMap);
        }

        return workspaceCodeMap;
    }


    /**
     * Processes full workspace codemap and returns consolidated markdown content for all packages.
     *
     * @param project          the Ballerina workspace project
     * @param workspaceManager the workspace manager
     * @return consolidated workspace markdown content
     */
    public static String processFullWorkspaceCodeMap(Project project, WorkspaceManager workspaceManager) {
        Map<String, Map<String, CodeMapFile>> workspaceCodeMap = generateWorkspaceCodeMap(project, workspaceManager);

        Path sourceRoot = project.sourceRoot();
        String workspaceName = "Unknown Workspace";
        if (sourceRoot != null) {
            Path fileName = sourceRoot.getFileName();
            if (fileName != null) {
                workspaceName = fileName.toString();
            }
        }
        return CodeMapMarkdownGenerator.generateWorkspaceMarkdown(workspaceCodeMap, workspaceName);
    }

    // Safely processes a single node with error handling
    private static void addArtifactSafely(io.ballerina.compiler.syntax.tree.Node node,
                                          CodeMapNodeTransformer transformer,
                                          List<CodeMapArtifact> artifacts) {
        if (hasErrorInNode(node)) {
            return; // Skip nodes that already have errors
        }

        try {
            Optional<CodeMapArtifact> artifact = node.apply(transformer);
            artifact.ifPresent(artifacts::add);
        } catch (RuntimeException e) {
            CodeMapArtifact errorArtifact = createGeneralSyntaxErrorArtifact(
                    "Error processing node: " + e.getMessage());
            artifacts.add(errorArtifact);
        } catch (Exception e) {
            CodeMapArtifact errorArtifact = createGeneralSyntaxErrorArtifact(
                    "Unexpected error processing node: " + e.getMessage());
            artifacts.add(errorArtifact);
        }
    }
}

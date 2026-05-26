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
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Package currentPackage = project.currentPackage();
        Map<String, CodeMapFile> codeMapFiles = new LinkedHashMap<>();
        String projectPath = project.sourceRoot().toAbsolutePath().toString();

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

    /**
     * Processes full project codeMap and returns consolidated markdown content.
     *
     * @param project          the Ballerina project
     * @param workspaceManager the workspace manager
     * @return consolidated project markdown content
     */
    public static String processPackageCodeMap(Project project, WorkspaceManager workspaceManager) {
        Map<String, CodeMapFile> codeMapFiles = generateCodeMap(project, workspaceManager);

        String projectName = project.currentPackage().packageName().value();

        return CodeMapMarkdownGenerator.generatePackageMarkdown(codeMapFiles, projectName);
    }

    /**
     * Processes full workspace codeMap and returns consolidated markdown content for all packages.
     *
     * @param project          the Ballerina workspace project
     * @param workspaceManager the workspace manager
     * @return consolidated workspace markdown content
     */
    public static String processWorkspaceCodeMap(Project project, WorkspaceManager workspaceManager) {
        Map<String, Map<String, CodeMapFile>> workspaceCodeMap = new LinkedHashMap<>();
        BallerinaCompilerApi compilerApi = BallerinaCompilerApi.getInstance();

        // For a single-package project, treat it as a one-entry workspace
        if (!compilerApi.isWorkspaceProject(project)) {
            String packageName = project.currentPackage().packageName().value();
            workspaceCodeMap.put(packageName, generateCodeMap(project, workspaceManager));
        } else {
            // Iterate packages in dependency order to preserve build ordering
            List<Project> workspaceProjects = compilerApi.getWorkspaceProjectsInOrder(project);
            for (Project packageProject : workspaceProjects) {
                String packageName = packageProject.currentPackage().packageName().value();
                workspaceCodeMap.put(packageName, generateCodeMap(packageProject, workspaceManager));
            }
        }

        // Derive workspace name from the source root directory
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

    // Collects code artifacts from syntax tree with error handling
    private static List<CodeMapArtifact> collectArtifactsFromSyntaxTree(String projectPath, SyntaxTree syntaxTree,
                                                                        SemanticModel semanticModel,
                                                                        ModuleInfo moduleInfo) {
        List<CodeMapArtifact> artifacts = new ArrayList<>();

        if (syntaxTree.hasDiagnostics()) {
            List<CodeMapArtifact> syntaxErrorArtifacts = CodeMapErrorHandler.createSyntaxErrorArtifacts(
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
        rootNode.imports().forEach(importNode -> CodeMapErrorHandler.addArtifactSafely(importNode,
                codeMapNodeTransformer, artifacts));

        // Process members individually with per-node error handling
        rootNode.members().forEach(member -> CodeMapErrorHandler.addArtifactSafely(member,
                codeMapNodeTransformer, artifacts));
        return artifacts;
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

}

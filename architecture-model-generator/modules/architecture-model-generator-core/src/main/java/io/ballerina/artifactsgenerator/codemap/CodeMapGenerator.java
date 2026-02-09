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
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

        for (var moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            ModuleInfo moduleInfo = ModuleInfo.from(module.descriptor());

            for (var documentId : module.documentIds()) {
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

                if (!artifacts.isEmpty()) {
                    CodeMapFile codeMapFile = new CodeMapFile(relativeFilePath, artifacts);
                    codeMapFiles.put(relativeFilePath, codeMapFile);
                }
            }
        }

        return codeMapFiles;
    }

    private static List<CodeMapArtifact> collectArtifactsFromSyntaxTree(String projectPath, SyntaxTree syntaxTree,
                                                                        SemanticModel semanticModel,
                                                                        ModuleInfo moduleInfo) {
        List<CodeMapArtifact> artifacts = new ArrayList<>();
        if (!syntaxTree.containsModulePart()) {
            return artifacts;
        }

        ModulePartNode rootNode = syntaxTree.rootNode();
        CodeMapNodeTransformer codeMapNodeTransformer = new CodeMapNodeTransformer(projectPath, semanticModel,
                moduleInfo);

        // Process imports
        rootNode.imports().stream()
                .map(importNode -> importNode.apply(codeMapNodeTransformer))
                .flatMap(Optional::stream)
                .forEach(artifacts::add);

        // Process other members (functions, services, types, etc.)
        rootNode.members().stream()
                .map(member -> member.apply(codeMapNodeTransformer))
                .flatMap(Optional::stream)
                .forEach(artifacts::add);

        return artifacts;
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
}

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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.designmodelgenerator.core.CommonUtils.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CodeMapGenerator {

    public static Map<String, CodeMapFile> generateCodeMap(Project project) {
        Package currentPackage = project.currentPackage();
        Map<String, CodeMapFile> codeMapFiles = new LinkedHashMap<>();
        String projectPath = project.sourceRoot().toAbsolutePath().toString();

        // Iterate through all modules (default module and submodules)
        for (var moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel =
                    PackageUtil.getCompilation(currentPackage).getSemanticModel(moduleId);
            ModuleInfo moduleInfo = createModuleInfo(module.descriptor());

            // Iterate through each document in the module
            for (var documentId : module.documentIds()) {
                Document document = module.document(documentId);
                SyntaxTree syntaxTree = document.syntaxTree();

                List<CodeMapArtifact> artifacts = collectArtifactsFromSyntaxTree(projectPath, syntaxTree,
                        semanticModel, moduleInfo);

                if (!artifacts.isEmpty()) {
                    String fileName = document.name();
                    String relativeFilePath = getRelativeFilePath(module, fileName);

                    CodeMapFile codeMapFile = new CodeMapFile(fileName, relativeFilePath, artifacts);
                    codeMapFiles.put(relativeFilePath, codeMapFile);
                }
            }
        }

        return codeMapFiles;
    }

    /**
     * Generate CodeMap for specific files only.
     *
     * @param project   the Ballerina project
     * @param fileNames list of file names to process (e.g., ["main.bal", "service.bal"])
     * @return map of relative file paths to CodeMapFile
     */
    public static Map<String, CodeMapFile> generateCodeMap(Project project, List<String> fileNames) {
        Package currentPackage = project.currentPackage();
        Map<String, CodeMapFile> codeMapFiles = new LinkedHashMap<>();
        String projectPath = project.sourceRoot().toAbsolutePath().toString();
        Set<String> targetFiles = new HashSet<>(fileNames);

        for (var moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
            SemanticModel semanticModel =
                    PackageUtil.getCompilation(currentPackage).getSemanticModel(moduleId);
            ModuleInfo moduleInfo = createModuleInfo(module.descriptor());

            for (var documentId : module.documentIds()) {
                Document document = module.document(documentId);
                String fileName = document.name();

                // Skip files not in the target list
                if (!targetFiles.contains(fileName)) {
                    continue;
                }

                SyntaxTree syntaxTree = document.syntaxTree();
                List<CodeMapArtifact> artifacts = collectArtifactsFromSyntaxTree(projectPath, syntaxTree,
                        semanticModel, moduleInfo);

                if (!artifacts.isEmpty()) {
                    String relativeFilePath = getRelativeFilePath(module, fileName);
                    CodeMapFile codeMapFile = new CodeMapFile(fileName, relativeFilePath, artifacts);
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

        rootNode.members().stream()
                .map(member -> member.apply(codeMapNodeTransformer))
                .flatMap(Optional::stream)
                .forEach(artifacts::add);

        return artifacts;
    }

    private static ModuleInfo createModuleInfo(ModuleDescriptor descriptor) {
        return new ModuleInfo(
                descriptor.org().value(),
                descriptor.packageName().value(),
                descriptor.name().toString(),
                descriptor.version().toString());
    }

    private static String getRelativeFilePath(Module module, String fileName) {
        // For submodules, the path is modules/<module-name>/<filename>
        // For default module, the path is just <filename>
        if (module.isDefaultModule()) {
            return fileName;
        }
        String moduleName = module.moduleName().moduleNamePart();
        return "modules" + File.separator + moduleName + File.separator + fileName;
    }
}
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
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CodeMapGenerator {

    public static List<CodeMapFile> generateCodeMap(Project project) {
        Package currentPackage = project.currentPackage();
        Module defaultModule = currentPackage.getDefaultModule();
        SemanticModel semanticModel =
                PackageUtil.getCompilation(currentPackage).getSemanticModel(defaultModule.moduleId());

        List<CodeMapFile> codeMapFiles = new ArrayList<>();
        String projectPath = project.sourceRoot().toAbsolutePath().toString();

        for (var documentId : defaultModule.documentIds()) {
            Document document = defaultModule.document(documentId);
            SyntaxTree syntaxTree = document.syntaxTree();

            List<CodeMapArtifact> artifacts = collectArtifactsFromSyntaxTree(projectPath, syntaxTree, semanticModel);

            if (!artifacts.isEmpty()) {
                String fileName = document.name();
                String absoluteFilePath = syntaxTree.filePath();

                CodeMapFile codeMapFile = new CodeMapFile(fileName, absoluteFilePath, artifacts);
                codeMapFiles.add(codeMapFile);
            }
        }

        return codeMapFiles;
    }

    public static CodeMapFile generateCodeMapForSyntaxTree(String projectPath, SyntaxTree syntaxTree,
                                                          SemanticModel semanticModel) {
        List<CodeMapArtifact> artifacts = collectArtifactsFromSyntaxTree(projectPath, syntaxTree, semanticModel);

        String fileName = syntaxTree.filePath().substring(syntaxTree.filePath().lastIndexOf('/') + 1);
        String absoluteFilePath = syntaxTree.filePath();

        return new CodeMapFile(fileName, absoluteFilePath, artifacts);
    }

    private static List<CodeMapArtifact> collectArtifactsFromSyntaxTree(String projectPath, SyntaxTree syntaxTree,
                                                                        SemanticModel semanticModel) {
        List<CodeMapArtifact> artifacts = new ArrayList<>();
        if (!syntaxTree.containsModulePart()) {
            return artifacts;
        }

        ModulePartNode rootNode = syntaxTree.rootNode();
        CodeMapNodeTransformer codeMapNodeTransformer = new CodeMapNodeTransformer(projectPath, semanticModel);

        rootNode.members().stream()
                .map(member -> member.apply(codeMapNodeTransformer))
                .flatMap(Optional::stream)
                .forEach(artifacts::add);

        return artifacts;
    }
}
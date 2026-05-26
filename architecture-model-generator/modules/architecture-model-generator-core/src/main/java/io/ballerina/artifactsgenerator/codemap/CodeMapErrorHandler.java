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

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.diagnostics.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles error detection and error artifact creation for the code map generation pipeline.
 *
 * @since 1.6.0
 */
class CodeMapErrorHandler {

    private CodeMapErrorHandler() {
    }

    // Creates artifacts for syntax errors found in diagnostics
    static List<CodeMapArtifact> createSyntaxErrorArtifacts(Iterable<Diagnostic> diagnostics,
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
    static CodeMapArtifact createGeneralSyntaxErrorArtifact(String errorMessage) {
        return new CodeMapArtifact(
                "Parsing Error",
                "SYNTAX_ERROR",
                null,
                Map.of("errorMessage", "Failed to parse file: " + errorMessage),
                Collections.emptyList()
        );
    }

    // Checks if a syntax node contains errors or missing tokens
    static boolean hasErrorInNode(Node node) {
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

    // Safely processes a single node with error handling
    static void addArtifactSafely(Node node, CodeMapNodeTransformer transformer,
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

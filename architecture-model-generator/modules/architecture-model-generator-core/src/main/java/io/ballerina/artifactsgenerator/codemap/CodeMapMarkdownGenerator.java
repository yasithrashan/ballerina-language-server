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

import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates markdown documentation from CodeMap data.
 * Converts structured code artifacts into readable markdown format for LLM consumption.
 *
 * @since 1.6.0
 */
public class CodeMapMarkdownGenerator {

    /**
     * Generates markdown for a single file.
     *
     * @param filePath the file path
     * @param codeMapFile the code map file data
     * @return the generated markdown string for the single file
     */
    public static String generateFileMarkdown(String filePath, CodeMapFile codeMapFile) {
        if (codeMapFile == null) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        lines.add("### " + filePath);

        if (codeMapFile.artifacts().isEmpty()) {
            return String.join("\n", lines);
        }

        // Group artifacts by type
        ArtifactGroups groups = new ArtifactGroups();
        categorizeArtifacts(codeMapFile.artifacts(), groups);

        // Render sections in order (only non-empty)
        renderCodeIssues(lines, groups.codeIssues);
        renderImports(lines, groups.imports);
        renderConfigurables(lines, groups.configurables);
        renderVariables(lines, groups.variables);
        renderTypes(lines, groups.types);
        renderFunctions(lines, groups.functions);
        renderAutomations(lines, groups.automations);
        renderListeners(lines, groups.listeners);
        renderConnections(lines, groups.connections);
        renderServices(lines, groups.services);
        renderClasses(lines, groups.classes);
        renderDataMappers(lines, groups.dataMappers);

        return String.join("\n", lines);
    }

    /**
     * Generates markdown from a code map response.
     *
     * @param files the code map files organized by file path
     * @return the generated markdown string
     */
    public static String generateMarkdown(Map<String, CodeMapFile> files) {
        return generateMarkdown(files, "Project");
    }

    /**
     * Generates markdown from a code map response with a custom project name.
     *
     * @param files the code map files organized by file path
     * @param projectName the name of the project/package
     * @return the generated markdown string
     */
    public static String generateMarkdown(Map<String, CodeMapFile> files, String projectName) {
        if (files == null || files.isEmpty()) {
            return "# " + projectName + " Codebase Summary\n\nNo files found.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# " + projectName + " Codebase Summary");

        for (Map.Entry<String, CodeMapFile> entry : files.entrySet()) {
            String filePath = entry.getKey();
            CodeMapFile fileData = entry.getValue();
            List<CodeMapArtifact> artifacts = fileData.artifacts();

            lines.add("");
            lines.add("---");
            lines.add("");
            // Extract just the filename from the full path
            String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1) : filePath;
            lines.add("## File Path : " + fileName);

            if (artifacts.isEmpty()) {
                continue;
            }

            // Group artifacts by type
            ArtifactGroups groups = new ArtifactGroups();
            categorizeArtifacts(artifacts, groups);

            // Render sections in order (only non-empty)
            renderCodeIssues(lines, groups.codeIssues);
            renderImports(lines, groups.imports);
            renderConfigurables(lines, groups.configurables);
            renderVariables(lines, groups.variables);
            renderTypes(lines, groups.types);
            renderFunctions(lines, groups.functions);
            renderAutomations(lines, groups.automations);
            renderListeners(lines, groups.listeners);
            renderConnections(lines, groups.connections);
            renderServices(lines, groups.services);
            renderClasses(lines, groups.classes);
            renderDataMappers(lines, groups.dataMappers);
        }

        lines.add("");
        return String.join("\n", lines);
    }

    private static void renderCodeIssues(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            String diagnosticMessage = getPropertyAsString(artifact, "diagnosticMessage", "");
            String errorMessage = getPropertyAsString(artifact, "errorMessage", "");
            String rawCode = getPropertyAsString(artifact, "rawCode", "");
            String errorCode = getPropertyAsString(artifact, "code", "");

            StringBuilder issueDescription = new StringBuilder();

            // Add [Parser Error] prefix for error codes < 2000
            if (!errorCode.isEmpty()) {
                try {
                    // Extract numeric part from error codes like "BCE1234"
                    String numericPart = errorCode.replaceAll("[^0-9]", "");
                    if (!numericPart.isEmpty()) {
                        int code = Integer.parseInt(numericPart);
                        if (code < 2000) {
                            issueDescription.append("[Parser Error] ");
                        }
                    }
                } catch (NumberFormatException e) {
                    // If parsing fails, continue without prefix
                }
            }

            // Format the issue description (without error codes)
            if (!diagnosticMessage.isEmpty()) {
                issueDescription.append(diagnosticMessage);
            } else if (!errorMessage.isEmpty()) {
                issueDescription.append(errorMessage);
            } else {
                issueDescription.append(artifact.name());
            }

            // Add line range
            issueDescription.append(" " + formatRange(artifact));

            // Add raw code if available in a code block
            if (!rawCode.isEmpty()) {
                lines.add("```ballerina");
                lines.add("// " + issueDescription.toString());
                lines.add(rawCode);
                lines.add("```");
            } else {
                lines.add("```ballerina");
                lines.add("// " + issueDescription.toString());
                lines.add("```");
            }
        }
    }

    private static void categorizeArtifacts(List<CodeMapArtifact> artifacts, ArtifactGroups groups) {
        for (CodeMapArtifact artifact : artifacts) {
            switch (artifact.type()) {
                case "SYNTAX_ERROR":
                    groups.codeIssues.add(artifact);
                    break;
                case "IMPORT":
                    groups.imports.add(artifact);
                    break;
                case "LISTENER":
                    groups.listeners.add(artifact);
                    break;
                case "TYPE":
                    groups.types.add(artifact);
                    break;
                case "SERVICE":
                    groups.services.add(artifact);
                    break;
                case "CLASS":
                    groups.classes.add(artifact);
                    break;
                case "DATA_MAPPER":
                    groups.dataMappers.add(artifact);
                    break;
                case "VARIABLE":
                    categorizeVariable(artifact, groups);
                    break;
                case "FUNCTION":
                    if ("main".equals(artifact.name())) {
                        groups.automations.add(artifact);
                    } else {
                        groups.functions.add(artifact);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void categorizeVariable(CodeMapArtifact artifact, ArtifactGroups groups) {
        String category = getPropertyAsString(artifact, "category", "").toUpperCase(Locale.ROOT);
        List<String> modifiers = getPropertyAsStringList(artifact, "modifiers");

        if ("CONFIGURABLE".equals(category) || modifiers.contains("configurable")) {
            groups.configurables.add(artifact);
        } else if ("CONNECTION".equals(category)) {
            groups.connections.add(artifact);
        } else {
            groups.variables.add(artifact);
        }
    }

    private static void renderImports(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            String org = getPropertyAsString(artifact, "orgName", "");
            String mod = getPropertyAsString(artifact, "moduleName", "");
            Object alias = artifact.properties().get("alias");
            StringBuilder entry = new StringBuilder("import ");
            entry.append(org.isEmpty() ? mod : org + "/" + mod);
            if (alias != null) {
                entry.append(" as ").append(alias);
            }
            entry.append(" " + formatRange(artifact));
            lines.add(entry.toString());
        }
        lines.add("```");
    }

    private static void renderConfigurables(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above configurable
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            StringBuilder configurableLine = new StringBuilder();
            configurableLine.append("configurable ");

            String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
            if (!typeDescriptor.isEmpty()) {
                configurableLine.append(typeDescriptor).append(" ");
            }

            configurableLine.append(artifact.name());

            String value = getPropertyAsString(artifact, "value", "");
            if (!value.isEmpty()) {
                configurableLine.append(" = ").append(value);
            }

            configurableLine.append(" " + formatRange(artifact));
            lines.add(configurableLine.toString());
        }
        lines.add("```");
    }

    private static void renderVariables(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above variable
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            StringBuilder variableLine = new StringBuilder();
            variableLine.append(modifiersPrefix(artifact));

            // Check if it's a constant (has both typeDescriptor and value properties)
            String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
            String value = getPropertyAsString(artifact, "value", "");
            boolean isConstant = !typeDescriptor.isEmpty() && !value.isEmpty();

            if (isConstant) {
                // Render as: const type name = value
                variableLine.append("const ").append(typeDescriptor).append(" ").append(artifact.name());
                variableLine.append(" = ").append(value);
            } else {
                // Render regular variables with type
                String type = getPropertyAsString(artifact, "type", "");
                if (!type.isEmpty()) {
                    variableLine.append(type).append(" ").append(artifact.name());
                } else {
                    variableLine.append(artifact.name());
                }
            }

            variableLine.append(" " + formatRange(artifact));
            lines.add(variableLine.toString());
        }
        lines.add("```");
    }

    private static void renderTypes(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above type
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
            StringBuilder typeLine = new StringBuilder(modifiersPrefix(artifact))
                .append("type ").append(artifact.name());
            if (!typeDescriptor.isEmpty()) {
                typeLine.append(" ").append(typeDescriptor);
            }
            typeLine.append(" " + formatRange(artifact));

            lines.add(typeLine.toString());
        }
        lines.add("```");
    }

    private static void renderFunctions(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above function
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            StringBuilder signature = new StringBuilder(modifiersPrefix(artifact))
                .append("function ").append(artifact.name());
            String params = parametersInline(artifact);
            String returns = getPropertyAsString(artifact, "returns", "()");

            // Always add parentheses, no space before opening parenthesis
            signature.append("(");
            if (!params.isEmpty()) {
                signature.append(params);
            }
            signature.append(")");

            if (!"()".equals(returns)) {
                signature.append(" returns ").append(returns);
            }
            signature.append(" " + formatRange(artifact));

            lines.add(signature.toString());
        }
        lines.add("```");
    }

    private static void renderAutomations(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            renderSingleFunctionInCodeBlock(lines, artifact, "");
        }
        lines.add("```");
    }

    private static void renderListeners(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above listener
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            StringBuilder listenerLine = new StringBuilder("listener ").append(artifact.name());
            String type = getPropertyAsString(artifact, "type", "");
            if (!type.isEmpty()) {
                listenerLine.append(" : ").append(type);
            }
            listenerLine.append(" " + formatRange(artifact));
            lines.add(listenerLine.toString());
        }
        lines.add("```");
    }

    private static void renderConnections(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above connection
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            lines.add(modifiersPrefix(artifact) + artifact.name() + " " + formatRange(artifact));
        }
        lines.add("```");
    }

    private static void renderServices(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above service
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            StringBuilder serviceLine = new StringBuilder()
                .append(modifiersPrefix(artifact))
                .append("service ")
                .append(artifact.name());
            String basePath = getPropertyAsString(artifact, "basePath", "");
            if (!basePath.isEmpty()) {
                serviceLine.append(" on ").append(basePath);
            }
            serviceLine.append(" { " + formatRange(artifact));
            lines.add(serviceLine.toString());

            if (!artifact.children().isEmpty()) {
                renderServiceChildrenInCodeBlock(lines, artifact.children());
            }

            lines.add("}");
        }
        lines.add("```");
    }


    private static void renderServiceChildrenInCodeBlock(List<String> lines, List<CodeMapArtifact> children) {
        for (CodeMapArtifact child : children) {
            if ("VARIABLE".equals(child.type()) || "FIELD".equals(child.type())) {
                String doc = getPropertyAsString(child, "documentation", "");
                if (!doc.isEmpty()) {
                    String[] docLines = doc.split("\\r?\\n");
                    for (String line : docLines) {
                        String trimmedLine = line.trim();
                        if (trimmedLine.isEmpty()) {
                            lines.add("    //");
                        } else {
                            lines.add("    // " + trimmedLine);
                        }
                    }
                }

                StringBuilder fieldLine = new StringBuilder("    ")
                    .append(modifiersPrefix(child));
                String type = getPropertyAsString(child, "type", "");
                if (!type.isEmpty()) {
                    fieldLine.append(type).append(" ").append(child.name());
                } else {
                    fieldLine.append(child.name());
                }
                fieldLine.append(" " + formatRange(child));
                lines.add(fieldLine.toString());
            } else if ("FUNCTION".equals(child.type())) {
                renderSingleFunctionInCodeBlock(lines, child, "    ");
            }
        }
    }

    private static void renderClasses(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            // Documentation (optional) - add as multi-line comment above class
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                String[] docLines = doc.split("\\r?\\n");
                for (String line : docLines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        lines.add("//");
                    } else {
                        lines.add("// " + trimmedLine);
                    }
                }
            }

            lines.add(modifiersPrefix(artifact) + "class " + artifact.name() + " { " + formatRange(artifact));

            if (!artifact.children().isEmpty()) {
                renderClassChildrenInCodeBlock(lines, artifact.children());
            }

            lines.add("}");
        }
        lines.add("```");
    }


    private static void renderClassChildrenInCodeBlock(List<String> lines, List<CodeMapArtifact> children) {
        for (CodeMapArtifact child : children) {
            if ("VARIABLE".equals(child.type()) || "FIELD".equals(child.type())) {
                String doc = getPropertyAsString(child, "documentation", "");
                if (!doc.isEmpty()) {
                    String[] docLines = doc.split("\\r?\\n");
                    for (String line : docLines) {
                        String trimmedLine = line.trim();
                        if (trimmedLine.isEmpty()) {
                            lines.add("    //");
                        } else {
                            lines.add("    // " + trimmedLine);
                        }
                    }
                }

                StringBuilder fieldLine = new StringBuilder("    ")
                    .append(modifiersPrefix(child));
                String type = getPropertyAsString(child, "type", "");
                if (!type.isEmpty()) {
                    fieldLine.append(type).append(" ").append(child.name());
                } else {
                    fieldLine.append(child.name());
                }
                fieldLine.append(" " + formatRange(child));
                lines.add(fieldLine.toString());
            } else if ("FUNCTION".equals(child.type())) {
                renderSingleFunctionInCodeBlock(lines, child, "    ");
            }
        }
    }

    private static void renderDataMappers(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            renderSingleFunctionInCodeBlock(lines, artifact, "");
        }
        lines.add("```");
    }


    private static void renderSingleFunctionInCodeBlock(List<String> lines, CodeMapArtifact artifact, String indent) {
        // Documentation (optional) - add as multi-line comment above function
        String doc = getPropertyAsString(artifact, "documentation", "");
        if (!doc.isEmpty()) {
            String[] docLines = doc.split("\\r?\\n");
            for (String line : docLines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    lines.add(indent + "//");
                } else {
                    lines.add(indent + "// " + trimmedLine);
                }
            }
        }

        // Build function signature
        StringBuilder signature = new StringBuilder(indent);

        String category = getPropertyAsString(artifact, "category", "").toUpperCase(Locale.ROOT);
        Object accessor = artifact.properties().get("accessor");
        boolean isResource = "RESOURCE".equals(category) || accessor != null;

        if (isResource) {
            signature.append("resource function ");
            String accessorStr = getPropertyAsString(artifact, "accessor", "");
            if (!accessorStr.isEmpty()) {
                signature.append(accessorStr).append(" ");
            }
            signature.append(artifact.name());
        } else {
            signature.append(modifiersPrefix(artifact)).append("function ");
            signature.append(artifact.name());
        }

        // Add parameters
        String params = parametersInline(artifact);
        signature.append("(");
        if (!params.isEmpty()) {
            signature.append(params);
        }
        signature.append(")");

        // Add returns
        String returns = getPropertyAsString(artifact, "returns", "()");
        if (!"()".equals(returns)) {
            signature.append(" returns ").append(returns);
        }

        signature.append(" " + formatRange(artifact));
        lines.add(signature.toString());
    }

    // Helper methods
    private static String getPropertyAsString(CodeMapArtifact artifact, String key, String fallback) {
        Object value = artifact.properties().get(key);
        return value != null ? value.toString() : fallback;
    }


    private static List<String> getPropertyAsStringList(CodeMapArtifact artifact, String key) {
        Object value = artifact.properties().get(key);
        if (value instanceof List) {
            try {
                return ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            } catch (ClassCastException e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private static String modifiersPrefix(CodeMapArtifact artifact) {
        List<String> mods = getPropertyAsStringList(artifact, "modifiers");
        if (mods.isEmpty()) {
            return "";
        }
        return String.join(" ", mods) + " ";
    }

    private static String parametersInline(CodeMapArtifact artifact) {
        List<?> params = (List<?>) artifact.properties().get("parameters");
        if (params == null || params.isEmpty()) {
            return "";
        }

        return params.stream()
            .map(p -> {
                if (p instanceof String) {
                    String paramStr = (String) p;
                    // Check if it's already in the correct format (type : name)
                    if (paramStr.contains(": ")) {
                        String[] parts = paramStr.split(": ", 2);
                        if (parts.length == 2) {
                            // Convert from "name: type" to "type : name"
                            return parts[1] + " : " + parts[0];
                        }
                    }
                    return paramStr;
                } else if (p instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramMap = (Map<String, Object>) p;
                    Object name = paramMap.get("name");
                    Object type = paramMap.get("type");
                    if (name != null && type != null) {
                        return type + " : " + name;
                    }
                }
                return p.toString();
            })
            .collect(Collectors.joining(", "));
    }

    private static String formatRange(CodeMapArtifact artifact) {
        Range range = artifact.range();
        if (range == null) {
            return "";
        }
        return String.format("[L:%d - L:%d]",
            range.getStart().getLine() + 1,
            range.getEnd().getLine() + 1);
    }



    /**
     * Generates markdown from a code map response with a custom project name and package prefix for file paths.
     *
     * @param files the code map files organized by file path
     * @param projectName the name of the project/package
     * @param packagePrefix the package name to prefix file paths with
     * @return the generated markdown string
     */
    public static String generateMarkdownWithPackagePrefix(Map<String, CodeMapFile> files, String projectName,
                                                           String packagePrefix) {
        if (files == null || files.isEmpty()) {
            return "# " + projectName + " Codebase Summary\n\nNo files found.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# " + projectName + " Codebase Summary");

        for (Map.Entry<String, CodeMapFile> entry : files.entrySet()) {
            String filePath = entry.getKey();
            CodeMapFile fileData = entry.getValue();
            List<CodeMapArtifact> artifacts = fileData.artifacts();

            lines.add("");
            lines.add("---");
            lines.add("");
            // Extract just the filename from the full path and prefix with package name
            String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1) : filePath;
            lines.add("## File Path : " + packagePrefix + "/" + fileName);

            if (artifacts.isEmpty()) {
                continue;
            }

            // Group artifacts by type
            ArtifactGroups groups = new ArtifactGroups();
            categorizeArtifacts(artifacts, groups);

            // Render sections in order (only non-empty)
            renderCodeIssues(lines, groups.codeIssues);
            renderImports(lines, groups.imports);
            renderConfigurables(lines, groups.configurables);
            renderVariables(lines, groups.variables);
            renderTypes(lines, groups.types);
            renderFunctions(lines, groups.functions);
            renderAutomations(lines, groups.automations);
            renderListeners(lines, groups.listeners);
            renderConnections(lines, groups.connections);
            renderServices(lines, groups.services);
            renderClasses(lines, groups.classes);
            renderDataMappers(lines, groups.dataMappers);
        }

        lines.add("");
        return String.join("\n", lines);
    }

    /**
     * Generates consolidated markdown for all packages in a workspace.
     *
     * @param workspaceCodeMap a map of package names to their code map files
     * @return the generated consolidated markdown string for the workspace
     */
    public static String generateWorkspaceMarkdown(Map<String, Map<String, CodeMapFile>> workspaceCodeMap) {
        return generateWorkspaceMarkdown(workspaceCodeMap, "Workspace");
    }

    /**
     * Generates consolidated markdown for all packages in a workspace with a custom workspace name.
     *
     * @param workspaceCodeMap a map of package names to their code map files
     * @param workspaceName the name of the workspace
     * @return the generated consolidated markdown string for the workspace
     */
    public static String generateWorkspaceMarkdown(Map<String, Map<String, CodeMapFile>> workspaceCodeMap,
                                                   String workspaceName) {
        if (workspaceCodeMap == null || workspaceCodeMap.isEmpty()) {
            return "# " + workspaceName + " Codebase Summary\n\nNo packages found in workspace.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# " + workspaceName + " Codebase Summary");

        // Process each package in the workspace
        for (Map.Entry<String, Map<String, CodeMapFile>> packageEntry : workspaceCodeMap.entrySet()) {
            String packageName = packageEntry.getKey();
            Map<String, CodeMapFile> packageFiles = packageEntry.getValue();

            if (packageFiles.isEmpty()) {
                continue;
            }

            // Add package header
            lines.add("");
            lines.add("---");
            lines.add("");
            lines.add("## Package: " + packageName);

            // Generate markdown for this package using existing method with package name
            String packageMarkdown = generateMarkdownWithPackagePrefix(packageFiles, packageName, packageName);

            // Remove the first line (package header) from package markdown to avoid duplicate headers
            String[] packageLines = packageMarkdown.split("\n");
            boolean skipFirstHeader = false;
            boolean skipInitialEmptyLines = false;
            for (String line : packageLines) {
                if (!skipFirstHeader && line.trim().startsWith("# " + packageName + " Codebase Summary")) {
                    skipFirstHeader = true;
                    skipInitialEmptyLines = true;
                    continue;
                }
                // Skip empty lines immediately after the header
                if (skipInitialEmptyLines && line.trim().isEmpty()) {
                    continue;
                } else {
                    skipInitialEmptyLines = false;
                }
                lines.add(line);
            }
        }

        lines.add("");
        return String.join("\n", lines);
    }

    /**
     * Renders multi-line documentation comments in the markdown format.
     * Handles documentation that contains newlines and formats it as proper multi-line comments.
     *
     * @param lines the output lines list to add formatted documentation to
     * @param artifact the artifact containing the documentation
     * @param indent the indentation to apply to each line
     */

    /**
     * Helper class to group artifacts by type.
     */
    private static class ArtifactGroups {
        final List<CodeMapArtifact> codeIssues = new ArrayList<>();
        final List<CodeMapArtifact> imports = new ArrayList<>();
        final List<CodeMapArtifact> configurables = new ArrayList<>();
        final List<CodeMapArtifact> connections = new ArrayList<>();
        final List<CodeMapArtifact> variables = new ArrayList<>();
        final List<CodeMapArtifact> types = new ArrayList<>();
        final List<CodeMapArtifact> functions = new ArrayList<>();
        final List<CodeMapArtifact> automations = new ArrayList<>();
        final List<CodeMapArtifact> listeners = new ArrayList<>();
        final List<CodeMapArtifact> services = new ArrayList<>();
        final List<CodeMapArtifact> classes = new ArrayList<>();
        final List<CodeMapArtifact> dataMappers = new ArrayList<>();
    }
}

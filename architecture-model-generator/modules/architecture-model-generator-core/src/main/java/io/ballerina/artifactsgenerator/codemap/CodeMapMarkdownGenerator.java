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
            return "# " + projectName + " Codebase Summary\n\nNo files found.\n";
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
            lines.add("## File Path : " + filePath);

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
        lines.add("### Code Issues");

        for (CodeMapArtifact artifact : artifacts) {
            String diagnosticMessage = getPropertyAsString(artifact, "diagnosticMessage", "");
            String errorMessage = getPropertyAsString(artifact, "errorMessage", "");
            String rawCode = getPropertyAsString(artifact, "rawCode", "");
            String errorCode = getPropertyAsString(artifact, "code", "");

            StringBuilder issueDescription = new StringBuilder();
            issueDescription.append("- ");

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
            issueDescription.append(getInlineRange(artifact));

            lines.add(issueDescription.toString());

            // Add raw code if available
            if (!rawCode.isEmpty()) {
                lines.add("  ```");
                lines.add("  " + rawCode);
                lines.add("  ```");
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
        lines.add("### Imports");

        for (CodeMapArtifact artifact : artifacts) {
            String org = getPropertyAsString(artifact, "orgName", "");
            String mod = getPropertyAsString(artifact, "moduleName", "");
            Object alias = artifact.properties().get("alias");
            StringBuilder entry = new StringBuilder(org.isEmpty() ? "- " + mod : "- " + org + "/" + mod);
            if (alias != null) {
                entry.append(" as ").append(alias);
            }
            entry.append(getInlineRange(artifact));
            lines.add(entry.toString());
        }
    }

    private static void renderConfigurables(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Configurables");

        for (CodeMapArtifact artifact : artifacts) {
            StringBuilder configurableLine = new StringBuilder();
            configurableLine.append("- configurable ");

            String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
            if (!typeDescriptor.isEmpty()) {
                configurableLine.append(typeDescriptor).append(" ");
            }

            configurableLine.append(artifact.name());

            String value = getPropertyAsString(artifact, "value", "");
            if (!value.isEmpty()) {
                configurableLine.append(" = ").append(value);
            }

            configurableLine.append(getInlineRange(artifact));
            lines.add(configurableLine.toString());

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }
        }
    }

    private static void renderVariables(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Variables");

        for (CodeMapArtifact artifact : artifacts) {
            StringBuilder variableLine = new StringBuilder();
            variableLine.append("- ").append(modifiersPrefix(artifact));

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

            variableLine.append(getInlineRange(artifact));
            lines.add(variableLine.toString());

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }
        }
    }

    private static void renderTypes(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Types");

        for (CodeMapArtifact artifact : artifacts) {
            String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
            StringBuilder typeLine = new StringBuilder(modifiersPrefix(artifact))
                .append("type ").append(artifact.name());
            if (!typeDescriptor.isEmpty()) {
                typeLine.append(" ").append(typeDescriptor);
            }

            lines.add("- " + typeLine + getInlineRange(artifact));

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }
        }
    }

    private static void renderFunctions(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Functions");

        for (CodeMapArtifact artifact : artifacts) {
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

            lines.add("- " + signature + getInlineRange(artifact));

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }
        }
    }

    private static void renderAutomations(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Automations (Entry Points)");

        for (CodeMapArtifact artifact : artifacts) {
            renderSingleFunction(lines, artifact, "", false);
        }
    }

    private static void renderListeners(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Listeners");

        for (CodeMapArtifact artifact : artifacts) {
            StringBuilder listenerLine = new StringBuilder("- listener ").append(artifact.name());
            String type = getPropertyAsString(artifact, "type", "");
            if (!type.isEmpty()) {
                listenerLine.append(" : ").append(type);
            }
            lines.add(listenerLine + getInlineRange(artifact));

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }
        }
    }

    private static void renderConnections(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Connections");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("- " + modifiersPrefix(artifact) + artifact.name() + getInlineRange(artifact));

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }
        }
    }

    private static void renderServices(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Services (Entry Points)");

        for (CodeMapArtifact artifact : artifacts) {
            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("// " + doc);
            }

            StringBuilder serviceLine = new StringBuilder("- ")
                .append(modifiersPrefix(artifact))
                .append("service ")
                .append(artifact.name());
            String basePath = getPropertyAsString(artifact, "basePath", "");
            if (!basePath.isEmpty()) {
                serviceLine.append(" on ").append(basePath);
            }
            lines.add(serviceLine + getInlineRange(artifact));

            if (!artifact.children().isEmpty()) {
                renderServiceChildren(lines, artifact.children());
            }
        }
    }

    private static void renderServiceChildren(List<String> lines, List<CodeMapArtifact> children) {
        List<CodeMapArtifact> fields = new ArrayList<>();
        List<CodeMapArtifact> resourceFns = new ArrayList<>();
        List<CodeMapArtifact> serviceFns = new ArrayList<>();

        for (CodeMapArtifact child : children) {
            if ("VARIABLE".equals(child.type()) || "FIELD".equals(child.type())) {
                fields.add(child);
            } else if ("FUNCTION".equals(child.type())) {
                String category = getPropertyAsString(child, "category", "").toUpperCase(Locale.ROOT);
                Object accessor = child.properties().get("accessor");
                if ("RESOURCE".equals(category) || accessor != null) {
                    resourceFns.add(child);
                } else {
                    serviceFns.add(child);
                }
            }
        }

        // Add Variables subsection if there are any fields
        if (!fields.isEmpty()) {
            lines.add("");
            lines.add("    #### Variables");
            for (CodeMapArtifact field : fields) {
                StringBuilder fieldLine = new StringBuilder("    - ")
                    .append(modifiersPrefix(field));
                String type = getPropertyAsString(field, "type", "");
                if (!type.isEmpty()) {
                    fieldLine.append(type).append(" ").append(field.name());
                } else {
                    fieldLine.append(field.name());
                }
                lines.add(fieldLine + getInlineRange(field));
            }
        }

        // Add Functions subsection if there are any functions
        if (!resourceFns.isEmpty() || !serviceFns.isEmpty()) {
            lines.add("");
            lines.add("    #### Functions");
            for (CodeMapArtifact fn : resourceFns) {
                renderSingleFunction(lines, fn, "    ", true);
            }

            for (CodeMapArtifact fn : serviceFns) {
                renderSingleFunction(lines, fn, "    ", false);
            }
        }
    }

    private static void renderClasses(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Classes");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("- " + modifiersPrefix(artifact) + "class " + artifact.name() + getInlineRange(artifact));

            String doc = getPropertyAsString(artifact, "documentation", "");
            if (!doc.isEmpty()) {
                lines.add("    - description: " + doc);
            }

            if (!artifact.children().isEmpty()) {
                renderClassChildren(lines, artifact.children());
            }
        }
    }

    private static void renderClassChildren(List<String> lines, List<CodeMapArtifact> children) {
        List<CodeMapArtifact> fields = new ArrayList<>();
        List<CodeMapArtifact> regularFns = new ArrayList<>();
        List<CodeMapArtifact> resourceFns = new ArrayList<>();
        List<CodeMapArtifact> remoteFns = new ArrayList<>();

        for (CodeMapArtifact child : children) {
            if ("VARIABLE".equals(child.type()) || "FIELD".equals(child.type())) {
                fields.add(child);
            } else if ("FUNCTION".equals(child.type())) {
                String cat = getPropertyAsString(child, "category", "").toUpperCase(Locale.ROOT);
                List<String> childMods = getPropertyAsStringList(child, "modifiers");
                Object accessor = child.properties().get("accessor");

                if ("RESOURCE".equals(cat) || accessor != null) {
                    resourceFns.add(child);
                } else if ("REMOTE".equals(cat) || childMods.contains("remote")) {
                    remoteFns.add(child);
                } else {
                    regularFns.add(child);
                }
            }
        }

        for (CodeMapArtifact field : fields) {
            StringBuilder fieldLine = new StringBuilder("  - ")
                .append(modifiersPrefix(field));
            String type = getPropertyAsString(field, "type", "");
            if (!type.isEmpty()) {
                fieldLine.append(type).append(" ").append(field.name());
            } else {
                fieldLine.append(field.name());
            }
            lines.add(fieldLine + getInlineRange(field));
        }

        for (CodeMapArtifact fn : regularFns) {
            renderSingleFunction(lines, fn, "  ", false);
        }

        for (CodeMapArtifact fn : resourceFns) {
            renderSingleFunction(lines, fn, "  ", true);
        }

        for (CodeMapArtifact fn : remoteFns) {
            renderSingleFunction(lines, fn, "  ", false);
        }
    }

    private static void renderDataMappers(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Data Mappers");

        for (CodeMapArtifact artifact : artifacts) {
            renderSingleFunction(lines, artifact, "", false);
        }
    }

    private static void renderSingleFunction(List<String> lines, CodeMapArtifact artifact,
                                              String indent, boolean isResource) {

        // Documentation (optional) - add as comment above function
        String doc = getPropertyAsString(artifact, "documentation", "");
        if (!doc.isEmpty()) {
            lines.add(indent + "// " + doc);
        }

        // Build function signature
        StringBuilder signature = new StringBuilder(indent).append("- ");
        if (isResource) {
            signature.append("resource function ");
            String accessor = getPropertyAsString(artifact, "accessor", "");
            if (!accessor.isEmpty()) {
                signature.append(accessor).append(" ");
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

        // Add line range
        signature.append(getInlineRange(artifact));
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


    private static String getInlineRange(CodeMapArtifact artifact) {
        String range = formatRange(artifact);
        return range.isEmpty() ? "" : " " + range;
    }

    /**
     * Generates consolidated markdown for all packages in a workspace.
     *
     * @param workspaceCodeMap a map of package names to their code map files
     * @return the generated consolidated markdown string for the workspace
     */
    public static String generateWorkspaceMarkdown(Map<String, Map<String, CodeMapFile>> workspaceCodeMap) {
        if (workspaceCodeMap == null || workspaceCodeMap.isEmpty()) {
            return "# Workspace Codebase Summary\n\nNo packages found in workspace.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# Workspace Codebase Summary");
        lines.add("");

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
            lines.add("# Package: " + packageName);
            lines.add("");

            // Generate markdown for this package using existing method with package name
            String packageMarkdown = generateMarkdown(packageFiles, packageName);

            // Remove the first line (package header) from package markdown to avoid duplicate headers
            String[] packageLines = packageMarkdown.split("\n");
            boolean skipFirstHeader = false;
            for (String line : packageLines) {
                if (!skipFirstHeader && line.trim().startsWith("# " + packageName + " Codebase Summary")) {
                    skipFirstHeader = true;
                    continue;
                }
                lines.add(line);
            }
        }

        lines.add("");
        return String.join("\n", lines);
    }

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

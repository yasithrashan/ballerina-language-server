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
 * Utility class for generating Markdown documentation from Ballerina code map artifacts.
 * This class provides functionality to create structured documentation in Markdown format
 * from parsed Ballerina source code artifacts including functions, types, services, etc.
 */
public class CodeMapMarkdownGenerator {

    /**
     * Main entry point for generating Markdown documentation from code map files.
     * Creates a structured document with proper section headers and separators.
     */
    public static String generateMarkdown(Map<String, CodeMapFile> files, String projectName) {
        if (files == null || files.isEmpty()) {
            return "# " + projectName + " Codebase Summary\n\nNo files found.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# " + projectName + " Codebase Summary");

        // Process each file and its artifacts
        for (Map.Entry<String, CodeMapFile> entry : files.entrySet()) {
            String filePath = entry.getKey();
            CodeMapFile fileData = entry.getValue();
            List<CodeMapArtifact> artifacts = fileData.artifacts();

            // Add file section with separator
            lines.add("");
            lines.add("---");
            lines.add("");
            lines.add("## File Path : " + filePath);

            if (!artifacts.isEmpty()) {
                renderArtifacts(lines, artifacts);
            }
        }

        lines.add("");
        return String.join("\n", lines);
    }

    /**
     * Generates Markdown documentation with a package prefix prepended to file paths.
     * This is useful for workspace-level documentation where file paths need to be
     * qualified with their package names.
     *
     * @param files         map of file paths to their code map data
     * @param projectName   the name to use in the document header
     * @param packagePrefix prefix to prepend to all file paths
     * @return Markdown string representation with prefixed file paths
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
            String fullPath = packagePrefix + "/" + filePath;
            lines.add("## File Path : " + fullPath);

            if (!artifacts.isEmpty()) {
                renderArtifacts(lines, artifacts);
            }
        }

        lines.add("");
        return String.join("\n", lines);
    }


    /**
     * Generates comprehensive workspace-level Markdown documentation.
     * Combines multiple packages into a single document with package sections.
     * Filters out redundant headers and empty lines for cleaner output.
     *
     * @param workspaceCodeMap nested map where keys are package names and values are file maps
     * @param workspaceName    the name to use in the document header
     * @return Markdown string representation of the entire workspace
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

            lines.add("");
            lines.add("---");
            lines.add("");
            lines.add("## Package: " + packageName);

            // Generate package content and filter out redundant headers
            String packageMarkdown = generateMarkdownWithPackagePrefix(packageFiles, packageName, packageName);
            String[] packageLines = packageMarkdown.split("\n");
            boolean skipFirstHeader = false;
            boolean skipInitialEmptyLines = false;
            for (String line : packageLines) {
                // Skip the package-level header as we already added it
                if (!skipFirstHeader && line.trim().startsWith("# " + packageName + " Codebase Summary")) {
                    skipFirstHeader = true;
                    skipInitialEmptyLines = true;
                    continue;
                }
                // Skip empty lines after header
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
     * Main artifact rendering orchestrator that processes all code artifacts in logical order.
     * Groups similar artifacts together and renders them in sections for optimal readability.
     */
    private static void renderArtifacts(List<String> lines, List<CodeMapArtifact> artifacts) {
        // Categorize artifacts by type for organized rendering
        ArtifactGroups groups = new ArtifactGroups();
        categorizeArtifacts(artifacts, groups);

        // Render artifacts in logical order: errors first, then structure, then code
        renderCodeIssues(lines, groups.codeIssues);
        renderCodeBlock(lines, groups.imports, CodeMapMarkdownGenerator::renderImport);
        renderCodeBlockWithDocs(lines, groups.configurables,
                CodeMapMarkdownGenerator::renderConfigurable);
        renderCodeBlockWithDocs(lines, groups.variables,
                CodeMapMarkdownGenerator::renderVariable);
        renderCodeBlockWithDocs(lines, groups.types,
                CodeMapMarkdownGenerator::renderType);
        renderCodeBlockWithDocs(lines, groups.functions, CodeMapMarkdownGenerator::renderFunction);
        renderCodeBlockWithDocs(lines, groups.automations,
                (artifact) -> renderSingleFunction(artifact, ""));
        renderCodeBlockWithDocs(lines, groups.listeners, CodeMapMarkdownGenerator::renderListener);
        renderCodeBlockWithDocs(lines, groups.connections,
                CodeMapMarkdownGenerator::renderConnection);
        renderServices(lines, groups.services);
        renderClasses(lines, groups.classes);
        renderCodeBlockWithDocs(lines, groups.dataMappers,
                (artifact) -> renderSingleFunction(artifact, ""));
    }

    /**
     * Renders artifacts as a simple Ballerina code block without documentation.
     * Used for imports and other structural elements that don't need API docs.
     */
    private static void renderCodeBlock(List<String> lines, List<CodeMapArtifact> artifacts,
                                        ArtifactRenderer renderer) {
        if (artifacts.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add("```ballerina");
        for (CodeMapArtifact artifact : artifacts) {
            lines.add(renderer.render(artifact));
        }
        lines.add("```");
    }

    /**
     * Renders artifacts with their API documentation as a Ballerina code block.
     * Includes comments, annotations, and documentation for comprehensive API reference.
     */
    private static void renderCodeBlockWithDocs(List<String> lines, List<CodeMapArtifact> artifacts,
                                                ArtifactRenderer renderer) {
        if (artifacts.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add("```ballerina");
        for (CodeMapArtifact artifact : artifacts) {
            renderApiDocumentation(lines, artifact, "");
            lines.add(renderer.render(artifact));
        }
        lines.add("```");
    }

    /**
     * Renders syntax errors and compilation issues with detailed diagnostic information.
     * Shows error messages, categories, and the problematic code when available.
     */
    private static void renderCodeIssues(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            // Extract error details from artifact properties
            String diagnosticMessage = getPropertyAsString(artifact, "diagnosticMessage", "");
            String errorMessage = getPropertyAsString(artifact, "errorMessage", "");
            String rawCode = getPropertyAsString(artifact, "rawCode", "");
            String errorCode = getPropertyAsString(artifact, "code", "");

            StringBuilder issueDescription = new StringBuilder();

            // Categorize error type based on error code
            if (!errorCode.isEmpty()) {
                try {
                    String numericPart = errorCode.replaceAll("[^0-9]", "");
                    if (!numericPart.isEmpty()) {
                        int code = Integer.parseInt(numericPart);
                        if (code < 2000) {
                            issueDescription.append("[Parser Error] ");
                        }
                    }
                } catch (NumberFormatException e) {
                }
            }

            // Build error description with fallback chain
            if (!diagnosticMessage.isEmpty()) {
                issueDescription.append(diagnosticMessage);
            } else if (!errorMessage.isEmpty()) {
                issueDescription.append(errorMessage);
            } else {
                issueDescription.append(artifact.name());
            }

            issueDescription.append(" ").append(formatRange(artifact));

            // Render as code block with error comment and problematic code
            lines.add("```ballerina");
            lines.add("// " + issueDescription);
            if (!rawCode.isEmpty()) {
                lines.add(rawCode);
            }
            lines.add("```");
        }
    }

    /**
     * Renders Ballerina service definitions with their nested resource functions and methods.
     * Services are rendered as hierarchical structures showing their complete API surface.
     */
    private static void renderServices(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            renderApiDocumentation(lines, artifact, "");

            // Build service declaration with modifiers, service path, and listener binding
            StringBuilder serviceLine = new StringBuilder()
                    .append(modifiersPrefix(artifact))
                    .append("service ");

            String serviceName = artifact.name();
            String servicePath = getPropertyAsString(artifact, "basePath", "");
            String listener = getPropertyAsString(artifact, "listener", "");

            // Add service name if present (e.g., http:Service)
            boolean hasServiceName = serviceName != null && !serviceName.isEmpty();
            if (hasServiceName) {
                serviceLine.append(serviceName);
            }

            // Add service path if present (e.g., /fhir/r4/metadata)
            if (!servicePath.isEmpty()) {
                if (hasServiceName) {
                    serviceLine.append(" ");
                }
                serviceLine.append(servicePath);
            }

            // Add listener if present (e.g., httpListener, new fhirr4:Listener(...))
            if (!listener.isEmpty()) {
                serviceLine.append(" on ").append(listener);
            }

            serviceLine.append(" { ").append(formatRange(artifact));
            lines.add(serviceLine.toString());

            // Render nested resource functions and methods
            if (!artifact.children().isEmpty()) {
                renderChildren(lines, artifact.children(), "    ");
            }

            lines.add("}");
        }
        lines.add("```");
    }

    /**
     * Renders Ballerina class definitions with their fields and methods.
     * Classes are rendered as hierarchical structures showing their complete interface.
     */
    private static void renderClasses(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            renderApiDocumentation(lines, artifact, "");
            lines.add(modifiersPrefix(artifact) + "class " + artifact.name() + " { " + formatRange(artifact));

            if (!artifact.children().isEmpty()) {
                renderChildren(lines, artifact.children(), "    ");
            }

            lines.add("}");
        }
        lines.add("```");
    }

    /**
     * Renders child artifacts (fields, methods, resources) with proper indentation.
     * Used for nested elements within services and classes.
     */
    private static void renderChildren(List<String> lines, List<CodeMapArtifact> children, String indent) {
        for (CodeMapArtifact child : children) {
            // Handle fields and variables
            if ("VARIABLE".equals(child.type()) || "FIELD".equals(child.type())) {
                renderApiDocumentation(lines, child, indent);

                StringBuilder fieldLine = new StringBuilder(indent)
                        .append(modifiersPrefix(child));
                String type = getPropertyAsString(child, "type", "");
                if (!type.isEmpty()) {
                    fieldLine.append(type).append(" ").append(child.name());
                } else {
                    fieldLine.append(child.name());
                }
                fieldLine.append(" ").append(formatRange(child));
                lines.add(fieldLine.toString());
            } else if ("FUNCTION".equals(child.type())) {
                // Handle methods and resource functions
                renderApiDocumentation(lines, child, indent);
                lines.add(renderSingleFunction(child, indent));
            } else if ("TYPE_INCLUSION".equals(child.type())) {
                // Handle object type inclusions (e.g., *persist:AbstractPersistClient;)
                renderApiDocumentation(lines, child, indent);
                lines.add(indent + child.name() + " " + formatRange(child));
            }
        }
    }

    /**
     * Renders function signatures with proper syntax for both regular and resource functions.
     * Resource functions use "resource function [method] [path]" syntax, regular functions use "function [name]".
     */
    private static String renderSingleFunction(CodeMapArtifact artifact, String indent) {
        StringBuilder signature = new StringBuilder(indent);

        // Determine if this is a resource function
        String category = getPropertyAsString(artifact, "category", "").toUpperCase(Locale.ROOT);
        Object accessor = artifact.properties().get("accessor");
        boolean isResource = "RESOURCE".equals(category) || accessor != null;

        // Build function signature based on type
        if (isResource) {
            // Resource function: "resource function [method] [path]"
            signature.append(modifiersPrefixExcluding(artifact, "resource"));
            signature.append("resource function ");
            String accessorStr = getPropertyAsString(artifact, "accessor", "");
            if (!accessorStr.isEmpty()) {
                signature.append(accessorStr).append(" ");
            }
            signature.append(artifact.name());
        } else {
            // Regular function: "function [name]"
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

        // Add return type if not void
        String returns = getPropertyAsString(artifact, "returns", "()");
        if (!"()".equals(returns)) {
            signature.append(" returns ").append(returns);
        }

        signature.append(" ").append(formatRange(artifact));
        return signature.toString();
    }

    private static String renderImport(CodeMapArtifact artifact) {
        String org = getPropertyAsString(artifact, "orgName", "");
        String mod = getPropertyAsString(artifact, "moduleName", "");
        Object alias = artifact.properties().get("alias");
        StringBuilder entry = new StringBuilder("import ");
        entry.append(org.isEmpty() ? mod : org + "/" + mod);
        if (alias != null) {
            entry.append(" as ").append(alias);
        }
        entry.append(" ").append(formatRange(artifact));
        return entry.toString();
    }

    private static String renderConfigurable(CodeMapArtifact artifact) {
        StringBuilder configurableLine = new StringBuilder("configurable ");
        String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
        String type = getPropertyAsString(artifact, "type", "");

        if (!typeDescriptor.isEmpty()) {
            configurableLine.append(typeDescriptor).append(" ");
        } else if (!type.isEmpty()) {
            configurableLine.append(type).append(" ");
        }

        configurableLine.append(artifact.name());
        String value = getPropertyAsString(artifact, "value", "");
        if (!value.isEmpty()) {
            configurableLine.append(" = ").append(value);
        }
        configurableLine.append(" ").append(formatRange(artifact));
        return configurableLine.toString();
    }

    private static String renderVariable(CodeMapArtifact artifact) {
        StringBuilder variableLine = new StringBuilder();

        String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
        String value = getPropertyAsString(artifact, "value", "");
        List<String> modifiers = getPropertyAsStringList(artifact, "modifiers");
        boolean isConstant = modifiers.contains("const");

        if (isConstant) {
            variableLine.append(modifiersPrefixExcluding(artifact, "const"));
            variableLine.append("const ");
            if (!typeDescriptor.isEmpty()) {
                variableLine.append(typeDescriptor).append(" ");
            }
            variableLine.append(artifact.name());
            if (!value.isEmpty()) {
                variableLine.append(" = ").append(value);
            }
        } else {
            variableLine.append(modifiersPrefix(artifact));
            String type = getPropertyAsString(artifact, "type", "");
            if (!type.isEmpty()) {
                variableLine.append(type).append(" ").append(artifact.name());
            } else {
                variableLine.append(artifact.name());
            }
        }

        variableLine.append(" ").append(formatRange(artifact));
        return variableLine.toString();
    }

    private static String renderType(CodeMapArtifact artifact) {
        String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
        StringBuilder typeLine = new StringBuilder(modifiersPrefix(artifact));

        // Handle enums differently from regular types
        if ("enum".equals(typeDescriptor)) {
            typeLine.append("enum ").append(artifact.name());
        } else {
            typeLine.append("type ").append(artifact.name());
            if (!typeDescriptor.isEmpty()) {
                typeLine.append(" ").append(typeDescriptor);
            }
        }

        typeLine.append(" ").append(formatRange(artifact));
        return typeLine.toString();
    }

    private static String renderFunction(CodeMapArtifact artifact) {
        StringBuilder signature = new StringBuilder(modifiersPrefix(artifact))
                .append("function ").append(artifact.name());
        String params = parametersInline(artifact);
        String returns = getPropertyAsString(artifact, "returns", "()");

        signature.append("(");
        if (!params.isEmpty()) {
            signature.append(params);
        }
        signature.append(")");

        if (!"()".equals(returns)) {
            signature.append(" returns ").append(returns);
        }
        signature.append(" ").append(formatRange(artifact));
        return signature.toString();
    }

    private static String renderListener(CodeMapArtifact artifact) {
        StringBuilder listenerLine = new StringBuilder("listener ");
        String type = getPropertyAsString(artifact, "type", "");
        if (!type.isEmpty()) {
            listenerLine.append(type).append(" ");
        }
        listenerLine.append(artifact.name());
        listenerLine.append(" ").append(formatRange(artifact));
        return listenerLine.toString();
    }

    private static String renderConnection(CodeMapArtifact artifact) {
        return modifiersPrefix(artifact) + artifact.name() + " " + formatRange(artifact);
    }

    /**
     * Renders documentation comments and annotations for artifacts in Ballerina comment format.
     * Preserves existing comment markers and adds proper indentation.
     */
    private static void renderApiDocumentation(List<String> lines, CodeMapArtifact artifact, String indent) {
        renderAnnotations(lines, artifact, indent);
        String doc = getPropertyAsString(artifact, "documentation", "");
        if (doc.isEmpty()) {
            return;
        }

        String[] docLines = doc.split("\\r?\\n", -1);

        for (String line : docLines) {
            String trimmedLine = line.trim();
            if (trimmedLine.equals("#")) {
                lines.add(indent + "#");
            } else if (!trimmedLine.isEmpty()) {
                if (trimmedLine.startsWith("# + ") || trimmedLine.startsWith("# - ")) {
                    lines.add(indent + trimmedLine);
                } else if (trimmedLine.startsWith("#")) {
                    lines.add(indent + trimmedLine);
                } else {
                    lines.add(indent + "# " + trimmedLine);
                }
            }
        }
    }

    private static void renderAnnotations(List<String> lines, CodeMapArtifact artifact, String indent) {
        List<String> annotations = getPropertyAsStringList(artifact, "annotations");
        for (String annotation : annotations) {
            lines.add(indent + annotation);
        }
    }

    private static void categorizeArtifacts(List<CodeMapArtifact> artifacts, ArtifactGroups groups) {
        // Sort artifacts into appropriate groups for organized rendering
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
                    // Variables need sub-categorization
                    categorizeVariable(artifact, groups);
                    break;
                case "FUNCTION":
                    // Separate main functions from regular functions
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
        // Sub-categorize variables based on their purpose and modifiers
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

    /**
     * Builds a modifier prefix string from artifact modifiers, excluding specific modifiers.
     *
     * @param artifact        the artifact to get modifiers from
     * @param excludeModifier the modifier to exclude from the prefix
     * @return space-separated modifiers with trailing space, or empty string
     */
    private static String modifiersPrefixExcluding(CodeMapArtifact artifact, String excludeModifier) {
        List<String> mods = getPropertyAsStringList(artifact, "modifiers");
        if (mods.isEmpty()) {
            return "";
        }
        List<String> filteredMods = mods.stream()
                .filter(mod -> !excludeModifier.equals(mod))
                .collect(Collectors.toList());
        if (filteredMods.isEmpty()) {
            return "";
        }
        return String.join(" ", filteredMods) + " ";
    }

    /**
     * Formats function parameters as an inline comma-separated string.
     * Parameters are already in the correct "type name" format from the transformer.
     *
     * @param artifact the function artifact containing parameters
     * @return comma-separated parameter string or empty string
     */
    private static String parametersInline(CodeMapArtifact artifact) {
        Object raw = artifact.properties().get("parameters");
        if (!(raw instanceof List)) {
            return "";
        }
        List<?> params = (List<?>) raw;
        if (params.isEmpty()) {
            return "";
        }

        return params.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    /**
     * Formats the source code line range for an artifact.
     * Converts zero-based LSP line numbers to one-based display format.
     *
     * @param artifact the artifact to get range from
     * @return formatted range string like "[L:5 - L:10]" or empty string
     */
    private static String formatRange(CodeMapArtifact artifact) {
        Range range = artifact.range();
        if (range == null) {
            return "";
        }
        // Convert 0-based LSP line numbers to 1-based display format
        return String.format("[L:%d - L:%d]",
                range.getStart().getLine() + 1,
                range.getEnd().getLine() + 1);
    }

    /**
     * Functional interface for rendering individual artifacts to strings.
     */
    @FunctionalInterface
    private interface ArtifactRenderer {
        String render(CodeMapArtifact artifact);
    }

    /**
     * Container class for organizing code artifacts into logical groups.
     * Each group corresponds to a different type of Ballerina language construct.
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

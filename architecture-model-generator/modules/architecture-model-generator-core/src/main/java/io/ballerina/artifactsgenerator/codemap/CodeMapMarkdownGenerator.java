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
     * Generates Markdown documentation for a single Ballerina source file.
     *
     * @param filePath the path to the source file
     * @param codeMapFile the parsed code map data for the file
     * @return Markdown string representation of the file's contents
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

        renderArtifacts(lines, codeMapFile.artifacts());
        return String.join("\n", lines);
    }

    /**
     * Generates Markdown documentation for multiple files using default project name.
     *
     * @param files map of file paths to their code map data
     * @return Markdown string representation of the project
     */
    public static String generateMarkdown(Map<String, CodeMapFile> files) {
        return generateMarkdown(files, "Project");
    }

    /**
     * Generates Markdown documentation for multiple files with a custom project name.
     * Creates a structured document with file sections separated by horizontal rules.
     *
     * @param files map of file paths to their code map data
     * @param projectName the name to use in the document header
     * @return Markdown string representation of the project
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
     * @param files map of file paths to their code map data
     * @param projectName the name to use in the document header
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
     * Generates workspace-level Markdown documentation using default workspace name.
     *
     * @param workspaceCodeMap nested map of package names to file maps
     * @return Markdown string representation of the entire workspace
     */
    public static String generateWorkspaceMarkdown(Map<String, Map<String, CodeMapFile>> workspaceCodeMap) {
        return generateWorkspaceMarkdown(workspaceCodeMap, "Workspace");
    }

    /**
     * Generates comprehensive workspace-level Markdown documentation.
     * Combines multiple packages into a single document with package sections.
     * Filters out redundant headers and empty lines for cleaner output.
     *
     * @param workspaceCodeMap nested map where keys are package names and values are file maps
     * @param workspaceName the name to use in the document header
     * @return Markdown string representation of the entire workspace
     */
    public static String generateWorkspaceMarkdown(Map<String, Map<String, CodeMapFile>> workspaceCodeMap,
                                                   String workspaceName) {
        if (workspaceCodeMap == null || workspaceCodeMap.isEmpty()) {
            return "# " + workspaceName + " Codebase Summary\n\nNo packages found in workspace.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# " + workspaceName + " Codebase Summary");

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

            // Generate package documentation and filter out redundant headers
            String packageMarkdown = generateMarkdownWithPackagePrefix(packageFiles, packageName, packageName);
            String[] packageLines = packageMarkdown.split("\n");
            boolean skipFirstHeader = false;
            boolean skipInitialEmptyLines = false;
            for (String line : packageLines) {
                if (!skipFirstHeader && line.trim().startsWith("# " + packageName + " Codebase Summary")) {
                    skipFirstHeader = true;
                    skipInitialEmptyLines = true;
                    continue;
                }
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
     * Renders all artifacts for a file by categorizing them into groups and
     * rendering each group in a specific order for optimal documentation structure.
     * Order: code issues, imports, configurables, variables, types, functions,
     * automations, listeners, connections, services, classes, data mappers.
     *
     * @param lines the list to append rendered markdown lines to
     * @param artifacts the list of code artifacts to render
     */
    private static void renderArtifacts(List<String> lines, List<CodeMapArtifact> artifacts) {
        ArtifactGroups groups = new ArtifactGroups();
        categorizeArtifacts(artifacts, groups);

        // Render artifacts in logical order for better documentation flow
        renderCodeIssues(lines, groups.codeIssues);
        renderCodeBlock(lines, groups.imports, CodeMapMarkdownGenerator::renderImport);
        renderCodeBlockWithDocs(lines, groups.configurables, CodeMapMarkdownGenerator::renderConfigurable);
        renderCodeBlockWithDocs(lines, groups.variables, CodeMapMarkdownGenerator::renderVariable);
        renderCodeBlockWithDocs(lines, groups.types, CodeMapMarkdownGenerator::renderType);
        renderCodeBlockWithDocs(lines, groups.functions, CodeMapMarkdownGenerator::renderFunction);
        renderCodeBlockWithDocs(lines, groups.automations, (artifact) -> renderSingleFunction(artifact, ""));
        renderCodeBlockWithDocs(lines, groups.listeners, CodeMapMarkdownGenerator::renderListener);
        renderCodeBlockWithDocs(lines, groups.connections, CodeMapMarkdownGenerator::renderConnection);
        renderServices(lines, groups.services);
        renderClasses(lines, groups.classes);
        renderCodeBlockWithDocs(lines, groups.dataMappers, (artifact) -> renderSingleFunction(artifact, ""));
    }

    /**
     * Renders a group of artifacts as a Ballerina code block without documentation comments.
     * Used for simple artifacts like imports that don't need API documentation.
     *
     * @param lines the list to append rendered markdown lines to
     * @param artifacts the artifacts to render
     * @param renderer the rendering function for individual artifacts
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
     * Renders a group of artifacts as a Ballerina code block with API documentation comments.
     * Includes any documentation comments that were present in the source code.
     *
     * @param lines the list to append rendered markdown lines to
     * @param artifacts the artifacts to render
     * @param renderer the rendering function for individual artifacts
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
     * Renders syntax errors and other code issues with detailed error information.
     * Categorizes errors as parser errors (code < 2000) and includes the problematic code snippet.
     *
     * @param lines the list to append rendered markdown lines to
     * @param artifacts the code issue artifacts to render
     */
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

            // Categorize error types based on error codes
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
                    // Continue without prefix if parsing fails
                }
            }

            // Use the most specific error message available
            if (!diagnosticMessage.isEmpty()) {
                issueDescription.append(diagnosticMessage);
            } else if (!errorMessage.isEmpty()) {
                issueDescription.append(errorMessage);
            } else {
                issueDescription.append(artifact.name());
            }

            issueDescription.append(" ").append(formatRange(artifact));

            lines.add("```ballerina");
            lines.add("// " + issueDescription);
            if (!rawCode.isEmpty()) {
                lines.add(rawCode);
            }
            lines.add("```");
        }
    }

    /**
     * Renders Ballerina service definitions with their resource functions and other members.
     * Services are rendered with their base paths and nested child artifacts (resources, functions).
     *
     * @param lines the list to append rendered markdown lines to
     * @param artifacts the service artifacts to render
     */
    private static void renderServices(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("```ballerina");

        for (CodeMapArtifact artifact : artifacts) {
            renderApiDocumentation(lines, artifact, "");

            // Build service declaration with optional base path
            StringBuilder serviceLine = new StringBuilder()
                .append(modifiersPrefix(artifact))
                .append("service ")
                .append(artifact.name());
            String basePath = getPropertyAsString(artifact, "basePath", "");
            if (!basePath.isEmpty()) {
                serviceLine.append(" on ").append(basePath);
            }
            serviceLine.append(" { ").append(formatRange(artifact));
            lines.add(serviceLine.toString());

            // Render nested artifacts (resource functions, etc.)
            if (!artifact.children().isEmpty()) {
                renderChildren(lines, artifact.children(), "    ");
            }

            lines.add("}");
        }
        lines.add("```");
    }

    /**
     * Renders Ballerina class definitions with their fields and methods.
     * Classes are rendered with their modifiers and nested child artifacts.
     *
     * @param lines the list to append rendered markdown lines to
     * @param artifacts the class artifacts to render
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

            // Render class members (fields, methods)
            if (!artifact.children().isEmpty()) {
                renderChildren(lines, artifact.children(), "    ");
            }

            lines.add("}");
        }
        lines.add("```");
    }

    /**
     * Renders child artifacts of services and classes with appropriate indentation.
     * Handles variables, fields, and functions as nested elements.
     *
     * @param lines the list to append rendered markdown lines to
     * @param children the child artifacts to render
     * @param indent the indentation string to use for nested elements
     */
    private static void renderChildren(List<String> lines, List<CodeMapArtifact> children, String indent) {
        for (CodeMapArtifact child : children) {
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
                renderApiDocumentation(lines, child, indent);
                lines.add(renderSingleFunction(child, indent));
            }
        }
    }

    /**
     * Renders a single function signature with appropriate formatting.
     * Handles both regular functions and resource functions with different syntax.
     * Includes annotations if present.
     *
     * @param artifact the function artifact to render
     * @param indent the indentation string for the function
     * @return the formatted function signature string
     */
    private static String renderSingleFunction(CodeMapArtifact artifact, String indent) {
        StringBuilder signature = new StringBuilder(indent);

        String category = getPropertyAsString(artifact, "category", "").toUpperCase(Locale.ROOT);
        Object accessor = artifact.properties().get("accessor");
        boolean isResource = "RESOURCE".equals(category) || accessor != null;

        // Build different syntax for resource vs regular functions
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

        // Add return type if present
        String returns = getPropertyAsString(artifact, "returns", "()");
        if (!"()".equals(returns)) {
            signature.append(" returns ").append(returns);
        }

        signature.append(" ").append(formatRange(artifact));
        return signature.toString();
    }

    /**
     * Renders an import statement with organization, module name and optional alias.
     *
     * @param artifact the import artifact to render
     * @return the formatted import statement string
     */
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

    /**
     * Renders a configurable variable declaration with type and optional default value.
     *
     * @param artifact the configurable artifact to render
     * @return the formatted configurable declaration string
     */
    private static String renderConfigurable(CodeMapArtifact artifact) {
        StringBuilder configurableLine = new StringBuilder("configurable ");
        String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
        if (!typeDescriptor.isEmpty()) {
            configurableLine.append(typeDescriptor).append(" ");
        }
        configurableLine.append(artifact.name());
        String value = getPropertyAsString(artifact, "value", "");
        if (!value.isEmpty()) {
            configurableLine.append(" = ").append(value);
        }
        configurableLine.append(" ").append(formatRange(artifact));
        return configurableLine.toString();
    }

    /**
     * Renders a variable declaration, handling both constants and regular variables.
     * Constants are detected by having both typeDescriptor and value properties.
     *
     * @param artifact the variable artifact to render
     * @return the formatted variable declaration string
     */
    private static String renderVariable(CodeMapArtifact artifact) {
        StringBuilder variableLine = new StringBuilder();
        variableLine.append(modifiersPrefix(artifact));

        String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
        String value = getPropertyAsString(artifact, "value", "");
        boolean isConstant = !typeDescriptor.isEmpty() && !value.isEmpty();

        if (isConstant) {
            variableLine.append("const ").append(typeDescriptor).append(" ").append(artifact.name());
            variableLine.append(" = ").append(value);
        } else {
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

    /**
     * Renders a type definition with its type descriptor.
     *
     * @param artifact the type artifact to render
     * @return the formatted type definition string
     */
    private static String renderType(CodeMapArtifact artifact) {
        String typeDescriptor = getPropertyAsString(artifact, "typeDescriptor", "");
        StringBuilder typeLine = new StringBuilder(modifiersPrefix(artifact))
            .append("type ").append(artifact.name());
        if (!typeDescriptor.isEmpty()) {
            typeLine.append(" ").append(typeDescriptor);
        }
        typeLine.append(" ").append(formatRange(artifact));
        return typeLine.toString();
    }

    /**
     * Renders a regular function signature with parameters and return type.
     * Includes annotations if present.
     *
     * @param artifact the function artifact to render
     * @return the formatted function signature string
     */
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

    /**
     * Renders a listener declaration with its type information.
     *
     * @param artifact the listener artifact to render
     * @return the formatted listener declaration string
     */
    private static String renderListener(CodeMapArtifact artifact) {
        StringBuilder listenerLine = new StringBuilder("listener ").append(artifact.name());
        String type = getPropertyAsString(artifact, "type", "");
        if (!type.isEmpty()) {
            listenerLine.append(" : ").append(type);
        }
        listenerLine.append(" ").append(formatRange(artifact));
        return listenerLine.toString();
    }

    /**
     * Renders a connection artifact (typically client connections).
     *
     * @param artifact the connection artifact to render
     * @return the formatted connection string
     */
    private static String renderConnection(CodeMapArtifact artifact) {
        return modifiersPrefix(artifact) + artifact.name() + " " + formatRange(artifact);
    }

    /**
     * Renders API documentation comments and annotations from artifact properties.
     * Preserves existing comment formatting and adds comment markers for plain text.
     * Handles multi-line documentation with proper indentation.
     *
     * @param lines the list to append documentation lines to
     * @param artifact the artifact containing documentation and annotations
     * @param indent the indentation string for the comments
     */
    private static void renderApiDocumentation(List<String> lines, CodeMapArtifact artifact, String indent) {
        // Render annotations first
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
                // Preserve existing comment formatting or add comment markers
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

    /**
     * Renders annotations for an artifact with proper indentation.
     * Annotations are displayed before the artifact declaration.
     *
     * @param lines the list to append annotation lines to
     * @param artifact the artifact containing annotations
     * @param indent the indentation string for the annotations
     */
    private static void renderAnnotations(List<String> lines, CodeMapArtifact artifact, String indent) {
        List<String> annotations = getPropertyAsStringList(artifact, "annotations");
        for (String annotation : annotations) {
            lines.add(indent + annotation);
        }
    }

    /**
     * Categorizes artifacts into logical groups for organized rendering.
     * Variables are further subcategorized based on their properties.
     * The 'main' function is treated as an automation rather than a regular function.
     *
     * @param artifacts the list of artifacts to categorize
     * @param groups the group container to populate
     */
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
                    // Special handling for main function as automation entry point
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

    /**
     * Subcategorizes variable artifacts based on their category and modifiers.
     * Separates configurables and connections from regular variables for better organization.
     *
     * @param artifact the variable artifact to categorize
     * @param groups the group container to add the artifact to
     */
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

    /**
     * Safely retrieves a string property from an artifact, with fallback value.
     *
     * @param artifact the artifact to get property from
     * @param key the property key
     * @param fallback the value to return if property is null
     * @return the property value as string or fallback
     */
    private static String getPropertyAsString(CodeMapArtifact artifact, String key, String fallback) {
        Object value = artifact.properties().get(key);
        return value != null ? value.toString() : fallback;
    }

    /**
     * Safely retrieves a list property from an artifact and converts to string list.
     *
     * @param artifact the artifact to get property from
     * @param key the property key
     * @return list of strings or empty list if property is null/invalid
     */
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

    /**
     * Builds a modifier prefix string from artifact modifiers (public, private, etc.).
     *
     * @param artifact the artifact to get modifiers from
     * @return space-separated modifiers with trailing space, or empty string
     */
    private static String modifiersPrefix(CodeMapArtifact artifact) {
        List<String> mods = getPropertyAsStringList(artifact, "modifiers");
        if (mods.isEmpty()) {
            return "";
        }
        return String.join(" ", mods) + " ";
    }

    /**
     * Formats function parameters as an inline comma-separated string.
     * Handles both string and map representations of parameters.
     * Reorders "name: type" format to "type : name" for Ballerina syntax.
     *
     * @param artifact the function artifact containing parameters
     * @return comma-separated parameter string or empty string
     */
    private static String parametersInline(CodeMapArtifact artifact) {
        List<?> params = (List<?>) artifact.properties().get("parameters");
        if (params == null || params.isEmpty()) {
            return "";
        }

        return params.stream()
            .map(p -> {
                if (p instanceof String) {
                    String paramStr = (String) p;
                    // Reorder "name: type" to "type : name" format
                    if (paramStr.contains(": ")) {
                        String[] parts = paramStr.split(": ", 2);
                        if (parts.length == 2) {
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
        final List<CodeMapArtifact> codeIssues = new ArrayList<>();      // Syntax errors and diagnostics
        final List<CodeMapArtifact> imports = new ArrayList<>();         // Import statements
        final List<CodeMapArtifact> configurables = new ArrayList<>();   // Configurable variables
        final List<CodeMapArtifact> connections = new ArrayList<>();     // Client connections
        final List<CodeMapArtifact> variables = new ArrayList<>();       // Regular variables
        final List<CodeMapArtifact> types = new ArrayList<>();           // Type definitions
        final List<CodeMapArtifact> functions = new ArrayList<>();       // Regular functions
        final List<CodeMapArtifact> automations = new ArrayList<>();     // Main function and automations
        final List<CodeMapArtifact> listeners = new ArrayList<>();       // Listener declarations
        final List<CodeMapArtifact> services = new ArrayList<>();        // Service definitions
        final List<CodeMapArtifact> classes = new ArrayList<>();         // Class definitions
        final List<CodeMapArtifact> dataMappers = new ArrayList<>();     // Data mapping functions
    }
}

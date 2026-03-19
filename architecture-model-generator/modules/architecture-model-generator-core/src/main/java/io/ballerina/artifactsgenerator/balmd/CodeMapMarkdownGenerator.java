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

package io.ballerina.artifactsgenerator.balmd;

import io.ballerina.artifactsgenerator.codemap.CodeMapArtifact;
import io.ballerina.artifactsgenerator.codemap.CodeMapFile;
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
     * Generates markdown from a code map response.
     *
     * @param files the code map files organized by file path
     * @return the generated markdown string
     */
    public static String generateMarkdown(Map<String, CodeMapFile> files) {
        if (files == null || files.isEmpty()) {
            return "# Project CodeMap\n\n## CodeMap Structure\n\nNo files found.\n";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# Project CodeMap");
        lines.add("");
        lines.add("## CodeMap Structure");
        lines.add("");
        lines.add("This document provides a structured overview of the project codebase.");
        lines.add("It is organized by file path and summarizes the following elements for each file.");
        lines.add("Each artifact is listed with its sub-properties on separate indented lines.");
        lines.add("");

        for (Map.Entry<String, CodeMapFile> entry : files.entrySet()) {
            String filePath = entry.getKey();
            CodeMapFile fileData = entry.getValue();
            List<CodeMapArtifact> artifacts = fileData.artifacts();

            if (artifacts.isEmpty()) {
                continue;
            }

            lines.add("");
            lines.add("---");
            lines.add("");
            lines.add("## File Path : " + filePath);

            // Group artifacts by type
            ArtifactGroups groups = new ArtifactGroups();
            categorizeArtifacts(artifacts, groups);

            // Render sections in order (only non-empty)
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

    private static void categorizeArtifacts(List<CodeMapArtifact> artifacts, ArtifactGroups groups) {
        for (CodeMapArtifact artifact : artifacts) {
            switch (artifact.type()) {
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
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            String org = getPropertyAsString(artifact, "orgName", "");
            String mod = getPropertyAsString(artifact, "moduleName", "");
            Object alias = artifact.properties().get("alias");

            lines.add("");
            StringBuilder entry = new StringBuilder(org.isEmpty() ? "- " + mod : "- " + org + "/" + mod);
            if (alias != null) {
                entry.append(" as ").append(alias);
            }
            lines.add(entry.toString());
            pushLineRange(lines, "  ", artifact);
        }
    }

    private static void renderConfigurables(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Configurables");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- configurable " + artifact.name());
            pushSubItem(lines, "  ", "Type", getPropertyAsString(artifact, "type", ""));
            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);
        }
    }

    private static void renderVariables(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Variables");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- " + modifiersPrefix(artifact) + artifact.name());
            pushSubItem(lines, "  ", "Type", getPropertyAsString(artifact, "type", ""));
            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);
        }
    }

    private static void renderTypes(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Types");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- type " + artifact.name());
            pushSubItem(lines, "  ", "Type Descriptor", getPropertyAsString(artifact, "typeDescriptor", ""));

            List<String> fields = getPropertyAsStringList(artifact, "fields");
            if (!fields.isEmpty()) {
                pushSubItemBracket(lines, "  ", "Fields", String.join(", ", fields));
            }

            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);
        }
    }

    private static void renderFunctions(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Functions");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            renderSingleFunction(lines, artifact, "", false);
        }
    }

    private static void renderAutomations(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Automations (Entry Points)");
        lines.add("");

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
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- listener " + artifact.name());
            pushSubItem(lines, "  ", "Type", getPropertyAsString(artifact, "type", ""));

            List<String> args = getPropertyAsStringList(artifact, "arguments");
            if (!args.isEmpty()) {
                pushSubItemBracket(lines, "  ", "Arguments", String.join(", ", args));
            }

            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);
        }
    }

    private static void renderConnections(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Connections");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- " + modifiersPrefix(artifact) + artifact.name());
            pushSubItem(lines, "  ", "Type", getPropertyAsString(artifact, "type", ""));
            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);
        }
    }

    private static void renderServices(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Services (Entry Points)");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- " + modifiersPrefix(artifact) + "service " + artifact.name());
            pushSubItem(lines, "  ", "Base Path", getPropertyAsString(artifact, "basePath", ""));
            pushSubItem(lines, "  ", "Listener Type", getPropertyAsString(artifact, "listenerType", ""));
            pushSubItem(lines, "  ", "Port", getPropertyAsString(artifact, "port", ""));
            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);

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

        for (CodeMapArtifact field : fields) {
            lines.add("");
            lines.add("  - " + modifiersPrefix(field) + field.name());
            pushSubItem(lines, "    ", "Type", getPropertyAsString(field, "type", ""));
            pushLineRange(lines, "    ", field);
        }

        for (CodeMapArtifact fn : resourceFns) {
            renderSingleFunction(lines, fn, "  ", true);
        }

        for (CodeMapArtifact fn : serviceFns) {
            renderSingleFunction(lines, fn, "  ", false);
        }
    }

    private static void renderClasses(List<String> lines, List<CodeMapArtifact> artifacts) {
        if (artifacts.isEmpty()) {
            return;
        }

        lines.add("");
        lines.add("### Classes");
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            lines.add("");
            lines.add("- " + modifiersPrefix(artifact) + "class " + artifact.name());
            pushSubItem(lines, "  ", "Description", getPropertyAsString(artifact, "documentation", ""));
            pushLineRange(lines, "  ", artifact);

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
            lines.add("");
            lines.add("  - " + modifiersPrefix(field) + field.name());
            pushSubItem(lines, "    ", "Type", getPropertyAsString(field, "type", ""));
            pushLineRange(lines, "    ", field);
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
        lines.add("");

        for (CodeMapArtifact artifact : artifacts) {
            renderSingleFunction(lines, artifact, "", false);
        }
    }

    private static void renderSingleFunction(List<String> lines, CodeMapArtifact artifact,
                                              String indent, boolean isResource) {
        String subIndent = indent + "  ";

        lines.add("");

        // Title line
        if (isResource) {
            String accessor = getPropertyAsString(artifact, "accessor", "");
            String accessorPrefix = accessor.isEmpty() ? "" : accessor + " ";
            String title = indent + "- " + accessorPrefix + "resource function " + artifact.name();
            lines.add(title);
        } else {
            lines.add(indent + "- " + modifiersPrefix(artifact) + "function " + artifact.name());
        }

        // Parameters
        String params = parametersInline(artifact);
        if (params.isEmpty()) {
            lines.add(subIndent + "- **Parameters**: none");
        } else {
            lines.add(subIndent + "- **Parameters**: [" + params + "]");
        }

        // Returns
        String returns = getPropertyAsString(artifact, "returns", "()");
        if ("()".equals(returns)) {
            lines.add(subIndent + "- **Returns**: ()");
        } else {
            lines.add(subIndent + "- **Returns**: [" + returns + "]");
        }

        // Documentation (optional)
        pushSubItem(lines, subIndent, "Description", getPropertyAsString(artifact, "documentation", ""));

        // Line Range
        pushLineRange(lines, subIndent, artifact);
    }

    // Helper methods
    private static String getPropertyAsString(CodeMapArtifact artifact, String key, String fallback) {
        Object value = artifact.properties().get(key);
        return value != null ? value.toString() : fallback;
    }

    @SuppressWarnings("unchecked")
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
                    return (String) p;
                } else if (p instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramMap = (Map<String, Object>) p;
                    Object name = paramMap.get("name");
                    Object type = paramMap.get("type");
                    if (name != null && type != null) {
                        return name + ": " + type;
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
        return String.format("(%d:%d-%d:%d)",
            range.getStart().getLine(), range.getStart().getCharacter(),
            range.getEnd().getLine(), range.getEnd().getCharacter());
    }

    private static void pushSubItem(List<String> lines, String indent, String label, String value) {
        if (value != null && !value.isEmpty()) {
            lines.add(indent + "- **" + label + "**: " + value);
        }
    }

    private static void pushSubItemBracket(List<String> lines, String indent, String label, String value) {
        if (value != null && !value.isEmpty()) {
            lines.add(indent + "- **" + label + "**: [" + value + "]");
        }
    }

    private static void pushLineRange(List<String> lines, String indent, CodeMapArtifact artifact) {
        String range = formatRange(artifact);
        if (!range.isEmpty()) {
            lines.add(indent + "- **Line Range**: " + range);
        }
    }

    /**
     * Helper class to group artifacts by type.
     */
    private static class ArtifactGroups {
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

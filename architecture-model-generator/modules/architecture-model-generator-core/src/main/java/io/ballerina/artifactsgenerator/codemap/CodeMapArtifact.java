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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.tools.text.LinePosition;

public record CodeMapArtifact(String name, String type, LineRange lineRange, List<String> modifiers,
                              Map<String, Object> properties, List<CodeMapArtifact> children) {

    public record LineRange(Position startLine, Position endLine) {
        public record Position(int line, int offset) {
            public static Position from(LinePosition linePosition) {
                return new Position(linePosition.line(), linePosition.offset());
            }
        }

        public static LineRange from(io.ballerina.tools.text.LineRange lineRange) {
            return new LineRange(
                    Position.from(lineRange.startLine()),
                    Position.from(lineRange.endLine())
            );
        }
    }

    public CodeMapArtifact {
        modifiers = modifiers == null ? Collections.emptyList() : Collections.unmodifiableList(modifiers);
        properties = properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
        children = children == null ? Collections.emptyList() : Collections.unmodifiableList(children);
    }

    public static class Builder {
        private String name;
        private String type;
        private LineRange lineRange;
        private final List<String> modifiers = new ArrayList<>();
        private final Map<String, Object> properties = new HashMap<>();
        private final List<CodeMapArtifact> children = new ArrayList<>();

        public Builder(Node node) {
            this.lineRange = LineRange.from(node.lineRange());
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder lineRange(io.ballerina.tools.text.LineRange lineRange) {
            this.lineRange = LineRange.from(lineRange);
            return this;
        }

        public Builder addModifier(String modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        public Builder modifiers(List<String> modifiers) {
            this.modifiers.clear();
            this.modifiers.addAll(modifiers);
            return this;
        }

        public Builder addProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder addChild(CodeMapArtifact child) {
            this.children.add(child);
            return this;
        }

        public Builder basePath(String basePath) {
            return addProperty("basePath", basePath);
        }

        public Builder port(String port) {
            return addProperty("port", port);
        }

        public Builder parameters(List<String> parameters) {
            return addProperty("parameters", parameters);
        }

        public Builder returns(String returnType) {
            return addProperty("returns", returnType);
        }

        public Builder fields(List<String> fields) {
            return addProperty("fields", fields);
        }

        public Builder endpoint(String endpoint) {
            return addProperty("endpoint", endpoint);
        }

        public Builder config(String config) {
            return addProperty("config", config);
        }

        public Builder line(int line) {
            return addProperty("line", line);
        }

        public Builder documentation(String documentation) {
            return addProperty("documentation", documentation);
        }

        public Builder comment(String comment) {
            return addProperty("comment", comment);
        }

        public CodeMapArtifact build() {
            return new CodeMapArtifact(name, type, lineRange, new ArrayList<>(modifiers),
                    new HashMap<>(properties), new ArrayList<>(children));
        }
    }
}
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
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExpressionFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeTransformer;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.modelgenerator.commons.CommonUtils.CONNECTOR_TYPE;
import static io.ballerina.modelgenerator.commons.CommonUtils.PERSIST;
import static io.ballerina.modelgenerator.commons.CommonUtils.PERSIST_MODEL_FILE;
import static io.ballerina.modelgenerator.commons.CommonUtils.getPersistModelFilePath;
import static io.ballerina.modelgenerator.commons.CommonUtils.isAiMemoryStore;
import static io.ballerina.modelgenerator.commons.CommonUtils.isAiKnowledgeBase;
import static io.ballerina.modelgenerator.commons.CommonUtils.isAiVectorStore;
import static io.ballerina.modelgenerator.commons.CommonUtils.isPersistClient;

public class CodeMapNodeTransformer extends NodeTransformer<Optional<CodeMapArtifact>> {

    private final SemanticModel semanticModel;
    private final String projectPath;

    private static final String AUTOMATION_FUNCTION_NAME = "automation";
    private static final String MAIN_FUNCTION_NAME = "main";

    public CodeMapNodeTransformer(String projectPath, SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.projectPath = projectPath;
    }

    @Override
    public Optional<CodeMapArtifact> transform(FunctionDefinitionNode functionDefinitionNode) {
        CodeMapArtifact.Builder functionBuilder = new CodeMapArtifact.Builder(functionDefinitionNode);
        String functionName = functionDefinitionNode.functionName().text();

        List<String> modifiers = extractModifiers(functionDefinitionNode.qualifierList());
        functionBuilder.modifiers(modifiers);

        List<String> parameters = extractParameters(functionDefinitionNode.functionSignature());
        functionBuilder.parameters(parameters);

        String returnType = extractReturnType(functionDefinitionNode.functionSignature());
        functionBuilder.returns(returnType);

        if (functionName.equals(MAIN_FUNCTION_NAME)) {
            functionBuilder
                    .name(AUTOMATION_FUNCTION_NAME)
                    .type("AUTOMATION");
        } else if (functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY) {
            if (BallerinaCompilerApi.getInstance()
                    .isNaturalExpressionBody((ExpressionFunctionBodyNode) functionDefinitionNode.functionBody())) {
                functionBuilder
                        .name(functionName)
                        .type("NP_FUNCTION");
            } else {
                functionBuilder
                        .name(functionName)
                        .type("DATA_MAPPER");
            }
        } else if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            String pathString = getPathString(functionDefinitionNode.relativeResourcePath());
            functionBuilder
                    .name(pathString)
                    .type("RESOURCE")
                    .addProperty("accessor", functionName);
        } else if (hasQualifier(functionDefinitionNode.qualifierList(), SyntaxKind.REMOTE_KEYWORD)) {
            functionBuilder
                    .name(functionName)
                    .type("REMOTE");
        } else {
            functionBuilder
                    .name(functionName)
                    .type("FUNCTION");
        }
        return Optional.of(functionBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ServiceDeclarationNode serviceDeclarationNode) {
        CodeMapArtifact.Builder serviceBuilder = new CodeMapArtifact.Builder(serviceDeclarationNode);

        SeparatedNodeList<ExpressionNode> expressions = serviceDeclarationNode.expressions();
        ExpressionNode firstExpression = expressions.isEmpty() ? null : expressions.get(0);

        Optional<TypeDescriptorNode> typeDescriptorNode = serviceDeclarationNode.typeDescriptor();
        NodeList<Node> resourcePaths = serviceDeclarationNode.absoluteResourcePath();

        String serviceName = determineServiceName(serviceDeclarationNode, typeDescriptorNode, resourcePaths, firstExpression);
        serviceBuilder.name(serviceName);

        String basePath = getPathString(resourcePaths);
        serviceBuilder.basePath(basePath);

        if (firstExpression != null) {
            extractPortFromExpression(firstExpression).ifPresent(serviceBuilder::port);
        }

        serviceBuilder.type("SERVICE");

        serviceDeclarationNode.members().forEach(member -> {
            member.apply(this).ifPresent(serviceBuilder::addChild);
        });

        return Optional.of(serviceBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ListenerDeclarationNode listenerDeclarationNode) {
        CodeMapArtifact.Builder listenerBuilder = new CodeMapArtifact.Builder(listenerDeclarationNode)
                .name(listenerDeclarationNode.variableName().text())
                .type("LISTENER");

        int line = listenerDeclarationNode.lineRange().startLine().line();
        listenerBuilder.line(line);

        listenerDeclarationNode.typeDescriptor().flatMap(semanticModel::symbol).ifPresent(symbol -> {
            if (symbol instanceof TypeSymbol typeSymbol) {
                listenerBuilder.addProperty("type", typeSymbol.signature());
            }
        });

        return Optional.of(listenerBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        CodeMapArtifact.Builder variableBuilder = new CodeMapArtifact.Builder(moduleVariableDeclarationNode)
                .name(CommonUtils.getVariableName(
                        moduleVariableDeclarationNode.typedBindingPattern().bindingPattern()));

        List<String> modifiers = extractModifiers(moduleVariableDeclarationNode.qualifiers());
        variableBuilder.modifiers(modifiers);

        int line = moduleVariableDeclarationNode.lineRange().startLine().line();
        variableBuilder.line(line);

        if (hasQualifier(moduleVariableDeclarationNode.qualifiers(), SyntaxKind.CONFIGURABLE_KEYWORD)) {
            variableBuilder.type("CONFIGURABLE");
        } else {
            Optional<ClassSymbol> connection = getConnection(moduleVariableDeclarationNode);
            if (connection.isPresent()) {
                variableBuilder
                        .type("CONNECTION")
                        .addProperty("type", connection.get().signature());
                if (isPersistClient(connection.get(), semanticModel)) {
                    variableBuilder.addProperty(CONNECTOR_TYPE, PERSIST);
                    getPersistModelFilePath(projectPath)
                            .ifPresent(modelFile -> variableBuilder.addProperty(PERSIST_MODEL_FILE, modelFile));
                }
            } else {
                variableBuilder.type("VARIABLE");
            }
        }

        semanticModel.symbol(moduleVariableDeclarationNode).ifPresent(symbol -> {
            if (symbol instanceof VariableSymbol variableSymbol) {
                variableBuilder.addProperty("type", variableSymbol.typeDescriptor().signature());
            }
        });

        return Optional.of(variableBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(TypeDefinitionNode typeDefinitionNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(typeDefinitionNode)
                .name(typeDefinitionNode.typeName().text())
                .type("TYPE");

        List<String> fields = extractFieldsFromTypeDefinition(typeDefinitionNode);
        typeBuilder.fields(fields);

        return Optional.of(typeBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(EnumDeclarationNode enumDeclarationNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(enumDeclarationNode)
                .name(enumDeclarationNode.identifier().text())
                .type("TYPE");
        return Optional.of(typeBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ClassDefinitionNode classDefinitionNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(classDefinitionNode)
                .name(classDefinitionNode.className().text())
                .type("TYPE");

        classDefinitionNode.members().forEach(member -> {
            member.apply(this).ifPresent(typeBuilder::addChild);
        });

        return Optional.of(typeBuilder.build());
    }

    @Override
    protected Optional<CodeMapArtifact> transformSyntaxNode(Node node) {
        return Optional.empty();
    }

    private List<String> extractModifiers(NodeList<Token> qualifierList) {
        return qualifierList.stream()
                .map(Token::text)
                .collect(Collectors.toList());
    }

    private List<String> extractParameters(FunctionSignatureNode functionSignature) {
        List<String> parameters = new ArrayList<>();
        SeparatedNodeList<ParameterNode> parameterNodes = functionSignature.parameters();

        for (ParameterNode paramNode : parameterNodes) {
            if (paramNode instanceof RequiredParameterNode requiredParam) {
                String paramType = requiredParam.typeName().toSourceCode().strip();
                String paramName = requiredParam.paramName().map(name -> name.text()).orElse("");
                parameters.add(paramName + ": " + paramType);
            } else {
                parameters.add(paramNode.toSourceCode().strip());
            }
        }
        return parameters;
    }

    private String extractReturnType(FunctionSignatureNode functionSignature) {
        return functionSignature.returnTypeDesc()
                .map(returnTypeDesc -> returnTypeDesc.type().toSourceCode().strip())
                .orElse("()");
    }

    private List<String> extractFieldsFromTypeDefinition(TypeDefinitionNode typeDefinitionNode) {
        List<String> fields = new ArrayList<>();
        semanticModel.symbol(typeDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof TypeSymbol typeSymbol &&
                typeSymbol.typeKind() == TypeDescKind.RECORD) {
                RecordTypeSymbol recordType = (RecordTypeSymbol) typeSymbol;
                for (RecordFieldSymbol field : recordType.fieldDescriptors().values()) {
                    fields.add(field.getName().orElse("") + ": " + field.typeDescriptor().signature());
                }
            }
        });
        return fields;
    }

    private String determineServiceName(ServiceDeclarationNode serviceDeclarationNode,
                                       Optional<TypeDescriptorNode> typeDescriptorNode,
                                       NodeList<Node> resourcePaths,
                                       ExpressionNode firstExpression) {
        if (typeDescriptorNode.isPresent()) {
            return typeDescriptorNode.get().toSourceCode().strip();
        } else if (!resourcePaths.isEmpty()) {
            return getPathString(resourcePaths);
        } else if (firstExpression != null) {
            return firstExpression.toSourceCode().strip();
        } else {
            return "";
        }
    }

    private Optional<String> extractPortFromExpression(ExpressionNode expression) {
        String expressionText = expression.toSourceCode().strip();
        if (expressionText.matches(".*\\d+.*")) {
            return Optional.of(expressionText.replaceAll("\\D", ""));
        }
        return Optional.empty();
    }

    private Optional<ClassSymbol> getConnection(Node node) {
        try {
            Symbol symbol = semanticModel.symbol(node).orElseThrow();
            TypeReferenceTypeSymbol typeDescriptorSymbol =
                    (TypeReferenceTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
            ClassSymbol classSymbol = (ClassSymbol) typeDescriptorSymbol.typeDescriptor();
            if (classSymbol.qualifiers().contains(Qualifier.CLIENT) || isAiKnowledgeBase(classSymbol)
                    || isAiVectorStore(symbol) || isAiMemoryStore(symbol)) {
                return Optional.of(classSymbol);
            }
        } catch (Throwable e) {
            // Ignore
        }
        return Optional.empty();
    }

    private static String getPathString(NodeList<Node> nodes) {
        return nodes.stream()
                .map(node -> node.toString().trim())
                .collect(Collectors.joining());
    }

    private static boolean hasQualifier(NodeList<Token> qualifierList, SyntaxKind kind) {
        return qualifierList.stream().anyMatch(qualifier -> qualifier.kind() == kind);
    }
}
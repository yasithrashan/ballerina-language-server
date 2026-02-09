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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeTransformer;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
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

/**
 * Transforms Ballerina syntax tree nodes into {@link CodeMapArtifact} instances.
 *
 * @since 1.6.0
 */
class CodeMapNodeTransformer extends NodeTransformer<Optional<CodeMapArtifact>> {

    private final SemanticModel semanticModel;
    private final String projectPath;
    private final ModuleInfo moduleInfo;
    private final boolean extractComments;

    private static final String MAIN_FUNCTION_NAME = "main";

    // Artifact type constants
    private static final String TYPE_FUNCTION = "FUNCTION";
    private static final String TYPE_SERVICE = "SERVICE";
    private static final String TYPE_IMPORT = "IMPORT";
    private static final String TYPE_LISTENER = "LISTENER";
    private static final String TYPE_VARIABLE = "VARIABLE";
    private static final String TYPE_TYPE = "TYPE";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";

    // Category constants
    private static final String CATEGORY_AUTOMATION = "AUTOMATION";
    private static final String CATEGORY_NP_FUNCTION = "NP_FUNCTION";
    private static final String CATEGORY_DATA_MAPPER = "DATA_MAPPER";
    private static final String CATEGORY_RESOURCE = "RESOURCE";
    private static final String CATEGORY_REMOTE = "REMOTE";
    private static final String CATEGORY_CONSTANT = "CONSTANT";
    private static final String CATEGORY_CONFIGURABLE = "CONFIGURABLE";
    private static final String CATEGORY_CONNECTION = "CONNECTION";
    private static final String CATEGORY_ENUM = "ENUM";
    private static final String CATEGORY_CLIENT = "CLIENT";

    // Property key constants
    private static final String PROP_PARAMETERS = "parameters";
    private static final String PROP_RETURNS = "returns";
    private static final String PROP_BASE_PATH = "basePath";
    private static final String PROP_PORT = "port";
    private static final String PROP_LISTENER_TYPE = "listenerType";
    private static final String PROP_ORG_NAME = "orgName";
    private static final String PROP_MODULE_NAME = "moduleName";
    private static final String PROP_ALIAS = "alias";
    private static final String PROP_TYPE = "type";
    private static final String PROP_ARGUMENTS = "arguments";
    private static final String PROP_TYPE_DESCRIPTOR = "typeDescriptor";
    private static final String PROP_VALUE = "value";
    private static final String PROP_FIELDS = "fields";
    private static final String PROP_ACCESSOR = "accessor";

    // Other constants
    private static final String RECORD_TYPE_NAME = "record";
    private static final String ALIAS_SEPARATOR = " as ";

    /**
     * Creates a new CodeMapNodeTransformer with comment extraction enabled.
     *
     * @param projectPath   the project root path
     * @param semanticModel the semantic model for symbol resolution
     * @param moduleInfo    the module information
     */
    CodeMapNodeTransformer(String projectPath, SemanticModel semanticModel, ModuleInfo moduleInfo) {
        this(projectPath, semanticModel, moduleInfo, true);
    }

    /**
     * Creates a new CodeMapNodeTransformer.
     *
     * @param projectPath     the project root path
     * @param semanticModel   the semantic model for symbol resolution
     * @param moduleInfo      the module information
     * @param extractComments whether to extract comments from nodes
     */
    CodeMapNodeTransformer(String projectPath, SemanticModel semanticModel, ModuleInfo moduleInfo,
                                  boolean extractComments) {
        this.semanticModel = semanticModel;
        this.projectPath = projectPath;
        this.moduleInfo = moduleInfo;
        this.extractComments = extractComments;
    }

    @Override
    public Optional<CodeMapArtifact> transform(FunctionDefinitionNode functionDefinitionNode) {
        CodeMapArtifact.Builder functionBuilder = new CodeMapArtifact.Builder(functionDefinitionNode);
        String functionName = functionDefinitionNode.functionName().text();

        List<String> modifiers = extractModifiers(functionDefinitionNode.qualifierList());
        functionBuilder.modifiers(modifiers);

        List<String> parameters = extractParameters(functionDefinitionNode.functionSignature());
        functionBuilder.addProperty(PROP_PARAMETERS, parameters);

        String returnType = extractReturnType(functionDefinitionNode.functionSignature());
        functionBuilder.addProperty(PROP_RETURNS, returnType);

        extractDocumentation(functionDefinitionNode.metadata()).ifPresent(functionBuilder::documentation);
        extractInlineComments(functionDefinitionNode).ifPresent(functionBuilder::comment);

        functionBuilder.type(TYPE_FUNCTION);

        if (functionName.equals(MAIN_FUNCTION_NAME)) {
            functionBuilder
                    .name(MAIN_FUNCTION_NAME)
                    .category(CATEGORY_AUTOMATION);
        } else if (functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY) {
            if (BallerinaCompilerApi.getInstance()
                    .isNaturalExpressionBody((ExpressionFunctionBodyNode) functionDefinitionNode.functionBody())) {
                functionBuilder
                        .name(functionName)
                        .category(CATEGORY_NP_FUNCTION);
            } else {
                functionBuilder
                        .name(functionName)
                        .category(CATEGORY_DATA_MAPPER);
            }
        } else if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            String pathString = getPathString(functionDefinitionNode.relativeResourcePath());
            functionBuilder
                    .name(pathString)
                    .category(CATEGORY_RESOURCE)
                    .addProperty(PROP_ACCESSOR, functionName);
        } else if (hasQualifier(functionDefinitionNode.qualifierList(), SyntaxKind.REMOTE_KEYWORD)) {
            functionBuilder
                    .name(functionName)
                    .category(CATEGORY_REMOTE);
        } else {
            functionBuilder.name(functionName);
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

        determineServiceName(serviceDeclarationNode, typeDescriptorNode, resourcePaths, firstExpression)
                .ifPresent(serviceBuilder::name);

        String basePath = getPathString(resourcePaths);
        serviceBuilder.addProperty(PROP_BASE_PATH, basePath);

        if (firstExpression != null) {
            extractPortFromExpression(firstExpression).ifPresent(port -> serviceBuilder.addProperty(PROP_PORT, port));
            extractListenerType(firstExpression).ifPresent(listenerType ->
                    serviceBuilder.addProperty(PROP_LISTENER_TYPE, listenerType));
        }

        serviceBuilder.type(TYPE_SERVICE);

        extractDocumentation(serviceDeclarationNode.metadata()).ifPresent(serviceBuilder::documentation);
        extractInlineComments(serviceDeclarationNode).ifPresent(serviceBuilder::comment);

        serviceDeclarationNode.members().forEach(member -> {
            member.apply(this).ifPresent(serviceBuilder::addChild);
        });

        return Optional.of(serviceBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ImportDeclarationNode importDeclarationNode) {
        // Extract org name
        String orgName = importDeclarationNode.orgName()
                .map(org -> org.orgName().text())
                .orElse("");

        // Extract module name
        String moduleName = importDeclarationNode.moduleName().stream()
                .map(Token::text)
                .collect(Collectors.joining("."));

        // Extract alias/prefix if present
        Optional<String> alias = importDeclarationNode.prefix()
                .map(prefix -> prefix.prefix().text());

        // Build full import name
        String fullImportName = orgName.isEmpty() ? moduleName : orgName + "/" + moduleName;
        if (alias.isPresent()) {
            fullImportName += ALIAS_SEPARATOR + alias.get();
        }

        CodeMapArtifact.Builder importBuilder = new CodeMapArtifact.Builder(importDeclarationNode)
                .name(fullImportName)
                .type(TYPE_IMPORT);

        // Add individual components as properties
        if (!orgName.isEmpty()) {
            importBuilder.addProperty(PROP_ORG_NAME, orgName);
        }
        importBuilder.addProperty(PROP_MODULE_NAME, moduleName);
        alias.ifPresent(a -> importBuilder.addProperty(PROP_ALIAS, a));

        extractInlineComments(importDeclarationNode).ifPresent(importBuilder::comment);

        return Optional.of(importBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ListenerDeclarationNode listenerDeclarationNode) {
        CodeMapArtifact.Builder listenerBuilder = new CodeMapArtifact.Builder(listenerDeclarationNode)
                .name(listenerDeclarationNode.variableName().text())
                .type(TYPE_LISTENER);

        listenerDeclarationNode.typeDescriptor().flatMap(semanticModel::symbol).ifPresent(symbol -> {
            if (symbol instanceof TypeSymbol typeSymbol) {
                listenerBuilder.addProperty(PROP_TYPE,
                        io.ballerina.designmodelgenerator.core.CommonUtils.getTypeSignature(typeSymbol, moduleInfo));
            }
        });

        // Extract initialization arguments
        Node initializer = listenerDeclarationNode.initializer();
        if (initializer instanceof NewExpressionNode newExpressionNode) {
            List<String> args = extractListenerArguments(newExpressionNode);
            if (!args.isEmpty()) {
                listenerBuilder.addProperty(PROP_ARGUMENTS, args);
            }
        }

        extractDocumentation(listenerDeclarationNode.metadata()).ifPresent(listenerBuilder::documentation);
        extractInlineComments(listenerDeclarationNode).ifPresent(listenerBuilder::comment);

        return Optional.of(listenerBuilder.build());
    }

    private List<String> extractListenerArguments(NewExpressionNode newExpressionNode) {
        List<String> arguments = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> argList = getArgList(newExpressionNode);

        for (FunctionArgumentNode argNode : argList) {
            if (argNode instanceof NamedArgumentNode namedArg) {
                String argName = namedArg.argumentName().name().text();
                String argValue = normalizeWhitespace(namedArg.expression().toSourceCode());
                arguments.add(argName + " = " + argValue);
            } else if (argNode instanceof PositionalArgumentNode positionalArg) {
                arguments.add(normalizeWhitespace(positionalArg.expression().toSourceCode()));
            }
        }
        return arguments;
    }

    private String normalizeWhitespace(String source) {
        // Replace newlines and multiple spaces with single space
        return source.replaceAll("\\s+", " ").strip();
    }

    private SeparatedNodeList<FunctionArgumentNode> getArgList(NewExpressionNode newExpressionNode) {
        if (newExpressionNode instanceof ExplicitNewExpressionNode explicitNew) {
            return explicitNew.parenthesizedArgList().arguments();
        } else if (newExpressionNode instanceof ImplicitNewExpressionNode implicitNew) {
            Optional<ParenthesizedArgList> argList = implicitNew.parenthesizedArgList();
            if (argList.isPresent()) {
                return argList.get().arguments();
            }
        }
        return NodeFactory.createSeparatedNodeList();
    }

    @Override
    public Optional<CodeMapArtifact> transform(ConstantDeclarationNode constantDeclarationNode) {
        CodeMapArtifact.Builder constantBuilder = new CodeMapArtifact.Builder(constantDeclarationNode)
                .name(constantDeclarationNode.variableName().text())
                .type(TYPE_VARIABLE)
                .category(CATEGORY_CONSTANT);

        // Extract the type descriptor
        constantDeclarationNode.typeDescriptor().ifPresent(typeDesc -> {
            String typeString = typeDesc.toSourceCode().strip();
            constantBuilder.addProperty(PROP_TYPE_DESCRIPTOR, typeString);
        });

        // Extract the constant value/initializer
        String value = constantDeclarationNode.initializer().toSourceCode().strip();
        constantBuilder.addProperty(PROP_VALUE, value);

        // Extract visibility qualifier (public, etc.)
        constantDeclarationNode.visibilityQualifier().ifPresent(visibility -> {
            constantBuilder.modifiers(List.of(visibility.text()));
        });

        extractDocumentation(constantDeclarationNode.metadata()).ifPresent(constantBuilder::documentation);
        extractInlineComments(constantDeclarationNode).ifPresent(constantBuilder::comment);

        return Optional.of(constantBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        CodeMapArtifact.Builder variableBuilder = new CodeMapArtifact.Builder(moduleVariableDeclarationNode)
                .name(CommonUtils.getVariableName(
                        moduleVariableDeclarationNode.typedBindingPattern().bindingPattern()));

        List<String> modifiers = extractModifiers(moduleVariableDeclarationNode.qualifiers());
        variableBuilder.modifiers(modifiers);

        variableBuilder.type(TYPE_VARIABLE);

        if (hasQualifier(moduleVariableDeclarationNode.qualifiers(), SyntaxKind.CONFIGURABLE_KEYWORD)) {
            variableBuilder.category(CATEGORY_CONFIGURABLE);
        } else {
            Optional<ClassSymbol> connection = getConnection(moduleVariableDeclarationNode);
            if (connection.isPresent()) {
                variableBuilder
                        .category(CATEGORY_CONNECTION)
                        .addProperty(PROP_TYPE, connection.get().signature());
                if (isPersistClient(connection.get(), semanticModel)) {
                    variableBuilder.addProperty(CONNECTOR_TYPE, PERSIST);
                    getPersistModelFilePath(projectPath)
                            .ifPresent(modelFile -> variableBuilder.addProperty(PERSIST_MODEL_FILE, modelFile));
                }
            }
        }

        semanticModel.symbol(moduleVariableDeclarationNode).ifPresent(symbol -> {
            if (symbol instanceof VariableSymbol variableSymbol) {
                variableBuilder.addProperty(PROP_TYPE,
                        io.ballerina.designmodelgenerator.core.CommonUtils.getTypeSignature(
                                variableSymbol.typeDescriptor(), moduleInfo));
            }
        });

        extractDocumentation(moduleVariableDeclarationNode.metadata()).ifPresent(variableBuilder::documentation);
        extractInlineComments(moduleVariableDeclarationNode).ifPresent(variableBuilder::comment);

        return Optional.of(variableBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(TypeDefinitionNode typeDefinitionNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(typeDefinitionNode)
                .name(typeDefinitionNode.typeName().text())
                .type(TYPE_TYPE);

        semanticModel.symbol(typeDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof TypeDefinitionSymbol typeDefSymbol) {
                TypeSymbol typeSymbol = typeDefSymbol.typeDescriptor();
                // For records (including intersection types like "readonly & record"),
                // just use "record" since fields are extracted separately
                String typeDescriptor = isRecordType(typeSymbol)
                        ? RECORD_TYPE_NAME
                        : io.ballerina.designmodelgenerator.core.CommonUtils.getTypeSignature(typeSymbol, moduleInfo);
                typeBuilder.addProperty(PROP_TYPE_DESCRIPTOR, typeDescriptor);
            }
        });

        List<String> fields = extractFieldsFromTypeDefinition(typeDefinitionNode);
        typeBuilder.addProperty(PROP_FIELDS, fields);

        extractDocumentation(typeDefinitionNode.metadata()).ifPresent(typeBuilder::documentation);
        extractInlineComments(typeDefinitionNode).ifPresent(typeBuilder::comment);

        return Optional.of(typeBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(EnumDeclarationNode enumDeclarationNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(enumDeclarationNode)
                .name(enumDeclarationNode.identifier().text())
                .type(TYPE_TYPE)
                .category(CATEGORY_ENUM);
        extractDocumentation(enumDeclarationNode.metadata()).ifPresent(typeBuilder::documentation);
        extractInlineComments(enumDeclarationNode).ifPresent(typeBuilder::comment);
        return Optional.of(typeBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ClassDefinitionNode classDefinitionNode) {
        NodeList<Token> classTypeQualifiers = classDefinitionNode.classTypeQualifiers();
        boolean isClientClass = hasQualifier(classTypeQualifiers, SyntaxKind.CLIENT_KEYWORD);

        CodeMapArtifact.Builder classBuilder = new CodeMapArtifact.Builder(classDefinitionNode)
                .name(classDefinitionNode.className().text())
                .type(TYPE_CLASS)
                .modifiers(extractModifiers(classDefinitionNode.visibilityQualifier(), classTypeQualifiers));

        if (isClientClass) {
            classBuilder.category(CATEGORY_CLIENT);
        }

        extractDocumentation(classDefinitionNode.metadata()).ifPresent(classBuilder::documentation);
        extractInlineComments(classDefinitionNode).ifPresent(classBuilder::comment);

        classDefinitionNode.members().forEach(member -> {
            member.apply(this).ifPresent(classBuilder::addChild);
        });

        return Optional.of(classBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ObjectFieldNode objectFieldNode) {
        String fieldName = objectFieldNode.fieldName().text();
        String fieldType = objectFieldNode.typeName().toSourceCode().strip();

        List<String> modifiers = new ArrayList<>();
        objectFieldNode.visibilityQualifier().ifPresent(token -> modifiers.add(token.text()));
        objectFieldNode.qualifierList().forEach(token -> modifiers.add(token.text()));

        CodeMapArtifact.Builder fieldBuilder = new CodeMapArtifact.Builder(objectFieldNode)
                .name(fieldName)
                .type(TYPE_FIELD)
                .modifiers(modifiers);

        fieldBuilder.addProperty(PROP_TYPE, fieldType);
        extractInlineComments(objectFieldNode).ifPresent(fieldBuilder::comment);

        return Optional.of(fieldBuilder.build());
    }

    private List<String> extractModifiers(Optional<Token> visibilityQualifier, NodeList<Token> classTypeQualifiers) {
        List<String> modifiers = new ArrayList<>();
        visibilityQualifier.ifPresent(token -> modifiers.add(token.text()));
        classTypeQualifiers.forEach(token -> modifiers.add(token.text()));
        return modifiers;
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
            } else if (paramNode instanceof DefaultableParameterNode defaultableParam) {
                String paramType = defaultableParam.typeName().toSourceCode().strip();
                String paramName = defaultableParam.paramName().map(name -> name.text()).orElse("");
                String defaultValue = defaultableParam.expression().toSourceCode().strip();
                parameters.add(paramName + ": " + paramType + " = " + defaultValue);
            } else if (paramNode instanceof RestParameterNode restParam) {
                String paramType = restParam.typeName().toSourceCode().strip();
                String paramName = restParam.paramName().map(name -> name.text()).orElse("");
                parameters.add(paramName + ": " + paramType + "...");
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
            if (symbol instanceof TypeDefinitionSymbol typeDefSymbol) {
                TypeSymbol typeSymbol = typeDefSymbol.typeDescriptor();
                RecordTypeSymbol recordType = getRecordTypeSymbol(typeSymbol);
                if (recordType != null) {
                    for (RecordFieldSymbol field : recordType.fieldDescriptors().values()) {
                        fields.add(field.getName().orElse("") + ": " +
                                io.ballerina.designmodelgenerator.core.CommonUtils.getTypeSignature(
                                        field.typeDescriptor(), moduleInfo));
                    }
                }
            }
        });
        return fields;
    }

    private boolean isRecordType(TypeSymbol typeSymbol) {
        return getRecordTypeSymbol(typeSymbol) != null;
    }

    private RecordTypeSymbol getRecordTypeSymbol(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
            return (RecordTypeSymbol) typeSymbol;
        }
        // Handle intersection types like "readonly & record"
        if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            IntersectionTypeSymbol intersectionType = (IntersectionTypeSymbol) typeSymbol;
            TypeSymbol effectiveType = intersectionType.effectiveTypeDescriptor();
            if (effectiveType.typeKind() == TypeDescKind.RECORD) {
                return (RecordTypeSymbol) effectiveType;
            }
        }
        return null;
    }

    private Optional<String> determineServiceName(ServiceDeclarationNode serviceDeclarationNode,
                                       Optional<TypeDescriptorNode> typeDescriptorNode,
                                       NodeList<Node> resourcePaths,
                                       ExpressionNode firstExpression) {
        if (typeDescriptorNode.isPresent()) {
            return Optional.of(typeDescriptorNode.get().toSourceCode().strip());
        } else if (!resourcePaths.isEmpty()) {
            return Optional.of(getPathString(resourcePaths));
        } else if (firstExpression != null) {
            return Optional.of(firstExpression.toSourceCode().strip());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractPortFromExpression(ExpressionNode expression) {
        String expressionText = expression.toSourceCode().strip();
        if (expressionText.matches(".*\\d+.*")) {
            return Optional.of(expressionText.replaceAll("\\D", ""));
        }
        return Optional.empty();
    }

    private Optional<String> extractListenerType(ExpressionNode expression) {
        if (expression instanceof ExplicitNewExpressionNode explicitNewExpr) {
            return semanticModel.symbol(explicitNewExpr.typeDescriptor())
                    .filter(symbol -> symbol instanceof TypeSymbol)
                    .map(symbol -> io.ballerina.designmodelgenerator.core.CommonUtils
                            .getTypeSignature((TypeSymbol) symbol, moduleInfo));
        }

        if (expression instanceof ImplicitNewExpressionNode) {
            return semanticModel.typeOf(expression)
                    .map(typeSymbol -> io.ballerina.designmodelgenerator.core.CommonUtils
                            .getTypeSignature(typeSymbol, moduleInfo));
        }
        return semanticModel.symbol(expression)
                .filter(symbol -> symbol instanceof VariableSymbol)
                .map(symbol -> ((VariableSymbol) symbol).typeDescriptor())
                .map(typeSymbol -> io.ballerina.designmodelgenerator.core.CommonUtils
                        .getTypeSignature(typeSymbol, moduleInfo));
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

    private Optional<String> extractDocumentation(Optional<MetadataNode> metadata) {
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        return metadata.get().documentationString()
                .filter(node -> node instanceof MarkdownDocumentationNode)
                .map(node -> {
                    MarkdownDocumentationNode docNode = (MarkdownDocumentationNode) node;
                    StringBuilder description = new StringBuilder();
                    for (Node documentationLine : docNode.documentationLines()) {
                        SyntaxKind lineKind = documentationLine.kind();
                        if (lineKind == SyntaxKind.MARKDOWN_DOCUMENTATION_LINE ||
                                lineKind == SyntaxKind.MARKDOWN_REFERENCE_DOCUMENTATION_LINE ||
                                lineKind == SyntaxKind.MARKDOWN_DEPRECATION_DOCUMENTATION_LINE) {
                            NodeList<Node> elements =
                                    ((MarkdownDocumentationLineNode) documentationLine).documentElements();
                            elements.forEach(element -> description.append(element.toSourceCode()));
                        }
                    }
                    return description.toString().strip();
                })
                .filter(doc -> !doc.isEmpty());
    }

    private Optional<String> extractInlineComments(Node node) {
        if (!extractComments) {
            return Optional.empty();
        }
        List<String> comments = new ArrayList<>();
        // Extract leading minutiae (comments before the node)
        node.leadingMinutiae().forEach(minutiae -> {
            if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                String commentText = minutiae.text().strip();
                // Remove the leading "//" and trim
                if (commentText.startsWith("//")) {
                    comments.add(commentText.substring(2).strip());
                }
            }
        });
        if (comments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(System.lineSeparator(), comments));
    }
}

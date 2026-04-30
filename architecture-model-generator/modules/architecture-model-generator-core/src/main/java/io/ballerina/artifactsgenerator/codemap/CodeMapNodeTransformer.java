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
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.EnumMemberNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
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
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;


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

    private static final String TYPE_FUNCTION = "FUNCTION";
    private static final String TYPE_SERVICE = "SERVICE";
    private static final String TYPE_IMPORT = "IMPORT";
    private static final String TYPE_LISTENER = "LISTENER";
    private static final String TYPE_VARIABLE = "VARIABLE";
    private static final String TYPE_TYPE = "TYPE";
    private static final String TYPE_CLASS = "CLASS";
    private static final String TYPE_FIELD = "FIELD";
    private static final String TYPE_INCLUSION = "TYPE_INCLUSION";

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
    private static final String PROP_ANNOTATIONS = "annotations";
    private static final String PROP_LISTENER = "listener";

    private static final String RECORD_TYPE_NAME = "record";
    private static final String ENUM_TYPE_NAME = "enum";
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

    /**
     * Transforms function definitions into CodeMapArtifact objects.
     * Handles both regular functions and resource functions with different naming strategies.
     */
    @Override
    public Optional<CodeMapArtifact> transform(FunctionDefinitionNode functionDefinitionNode) {
        CodeMapArtifact.Builder functionBuilder = new CodeMapArtifact.Builder(functionDefinitionNode);
        String functionName = functionDefinitionNode.functionName().text();

        // Extract function metadata (modifiers, parameters, return type)
        List<String> modifiers = extractModifiers(functionDefinitionNode.qualifierList());
        functionBuilder.modifiers(modifiers);

        List<String> parameters = extractParameters(functionDefinitionNode.functionSignature());
        functionBuilder.addProperty(PROP_PARAMETERS, parameters);

        String returnType = extractReturnType(functionDefinitionNode.functionSignature());
        functionBuilder.addProperty(PROP_RETURNS, returnType);

        extractDocumentation(functionDefinitionNode.metadata()).ifPresent(functionBuilder::documentation);
        extractInlineComments(functionDefinitionNode).ifPresent(functionBuilder::comment);

        List<String> annotations = extractAnnotations(functionDefinitionNode.metadata());
        if (!annotations.isEmpty()) {
            functionBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

        functionBuilder.type(TYPE_FUNCTION);

        // Handle resource functions differently - use path as name and store HTTP method
        if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            String pathString = getPathString(functionDefinitionNode.relativeResourcePath());
            String httpMethod = extractHttpMethodFromResourceFunction(functionDefinitionNode);
            functionBuilder
                    .name(pathString)
                    .addProperty(PROP_ACCESSOR, httpMethod);
        } else {
            functionBuilder.name(functionName);
        }
        return Optional.of(functionBuilder.build());
    }

    /**
     * Transforms service declarations into CodeMapArtifact objects.
     * Handles service name determination and listener configuration extraction.
     */
    @Override
    public Optional<CodeMapArtifact> transform(ServiceDeclarationNode serviceDeclarationNode) {
        CodeMapArtifact.Builder serviceBuilder = new CodeMapArtifact.Builder(serviceDeclarationNode);

        // Extract service components: listener expressions, type descriptor, and resource paths
        SeparatedNodeList<ExpressionNode> expressions = serviceDeclarationNode.expressions();
        ExpressionNode firstExpression = expressions.isEmpty() ? null : expressions.get(0);

        Optional<TypeDescriptorNode> typeDescriptorNode = serviceDeclarationNode.typeDescriptor();
        NodeList<Node> resourcePaths = serviceDeclarationNode.absoluteResourcePath();

        // Determine service name using multiple strategies
        Optional<String> serviceName = determineServiceName(serviceDeclarationNode, typeDescriptorNode,
                resourcePaths, firstExpression);
        serviceName.ifPresent(serviceBuilder::name);

        // Extract service path and listener information separately
        String servicePath = "";
        String listener = "";

        if (!resourcePaths.isEmpty()) {
            servicePath = getPathString(resourcePaths);
            if (firstExpression != null) {
                listener = safeExtractSourceCode(firstExpression);
            }
        } else if (firstExpression != null) {
            listener = safeExtractSourceCode(firstExpression);
        }

        // Store service path as basePath for backward compatibility and listener separately
        serviceBuilder.addProperty(PROP_BASE_PATH, servicePath);
        if (!listener.isEmpty()) {
            serviceBuilder.addProperty(PROP_LISTENER, listener);
        }

        // Extract listener configuration (port and type)
        if (firstExpression != null) {
            extractPortFromExpression(firstExpression).ifPresent(port -> serviceBuilder.addProperty(PROP_PORT, port));
            extractListenerType(firstExpression).ifPresent(listenerType ->
                    serviceBuilder.addProperty(PROP_LISTENER_TYPE, listenerType));
        }

        serviceBuilder.type(TYPE_SERVICE);

        extractDocumentation(serviceDeclarationNode.metadata()).ifPresent(serviceBuilder::documentation);
        extractInlineComments(serviceDeclarationNode).ifPresent(serviceBuilder::comment);

        List<String> annotations = extractAnnotations(serviceDeclarationNode.metadata());
        if (!annotations.isEmpty()) {
            serviceBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

        // Transform and add all service members as children
        serviceDeclarationNode.members().forEach(member -> {
            member.apply(this).ifPresent(serviceBuilder::addChild);
        });

        return Optional.of(serviceBuilder.build());
    }

    /**
     * Transforms import declarations into CodeMapArtifact objects.
     * Handles organization names, module names, and import aliases.
     */
    @Override
    public Optional<CodeMapArtifact> transform(ImportDeclarationNode importDeclarationNode) {
        // Parse import components: org, module, and alias
        String orgName = importDeclarationNode.orgName()
                .map(org -> org.orgName().text())
                .orElse("");

        String moduleName = importDeclarationNode.moduleName().stream()
                .map(Token::text)
                .collect(Collectors.joining("."));

        Optional<String> alias = importDeclarationNode.prefix()
                .map(prefix -> prefix.prefix().text());

        // Build full import name: "org/module as alias" or just "module"
        String fullImportName = orgName.isEmpty() ? moduleName : orgName + "/" + moduleName;
        if (alias.isPresent()) {
            fullImportName += ALIAS_SEPARATOR + alias.get();
        }

        CodeMapArtifact.Builder importBuilder = new CodeMapArtifact.Builder(importDeclarationNode)
                .name(fullImportName)
                .type(TYPE_IMPORT);

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
                        CommonUtils.getTypeSignature(typeSymbol, moduleInfo));
            }
        });

        Node initializer = listenerDeclarationNode.initializer();
        if (initializer instanceof NewExpressionNode newExpressionNode) {
            List<String> args = extractListenerArguments(newExpressionNode);
            if (!args.isEmpty()) {
                listenerBuilder.addProperty(PROP_ARGUMENTS, args);
            }
        }

        extractDocumentation(listenerDeclarationNode.metadata()).ifPresent(listenerBuilder::documentation);
        extractInlineComments(listenerDeclarationNode).ifPresent(listenerBuilder::comment);

        List<String> annotations = extractAnnotations(listenerDeclarationNode.metadata());
        if (!annotations.isEmpty()) {
            listenerBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

        return Optional.of(listenerBuilder.build());
    }

    private List<String> extractListenerArguments(NewExpressionNode newExpressionNode) {
        List<String> arguments = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> argList = getArgList(newExpressionNode);

        for (FunctionArgumentNode argNode : argList) {
            if (argNode == null) {
                continue;
            }

            try {
                if (argNode instanceof NamedArgumentNode namedArg) {
                    String argName = namedArg.argumentName().name().text();
                    String argValue = safeExtractSourceCode(namedArg.expression());
                    arguments.add(argName + " = " + argValue);
                } else if (argNode instanceof PositionalArgumentNode positionalArg) {
                    String argValue = safeExtractSourceCode(positionalArg.expression());
                    if (!argValue.isEmpty()) {
                        arguments.add(argValue);
                    }
                }
            } catch (RuntimeException e) {
                continue;
            }
        }
        return arguments;
    }

    private String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").strip();
    }

    /**
     * Extracts the HTTP method from a resource function definition.
     * Uses multiple strategies: parsing function signature, reflection, and fallback to function name.
     */
    private String extractHttpMethodFromResourceFunction(FunctionDefinitionNode functionDefinitionNode) {
        // Strategy 1: Parse function signature for HTTP method tokens
        FunctionSignatureNode functionSignature = functionDefinitionNode.functionSignature();
        if (functionSignature != null && functionSignature.children() != null) {
            for (Node child : functionSignature.children()) {
                if (child.kind() == SyntaxKind.IDENTIFIER_TOKEN) {
                    String text = child.toString().trim();
                    if (text.matches("get|post|put|patch|delete|head|options")) {
                        return text;
                    }
                }
            }
        }

        // Strategy 2: Use reflection to access internal API
        try {
            java.lang.reflect.Method method = functionDefinitionNode.getClass().getMethod("resourceAccessorName");
            Object accessorName = method.invoke(functionDefinitionNode);
            if (accessorName != null) {
                return accessorName.toString().trim();
            }
        } catch (ReflectiveOperationException e) {
        }

        // Fallback: use function name
        return functionDefinitionNode.functionName().text();
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
                .type(TYPE_VARIABLE);

        constantDeclarationNode.typeDescriptor().ifPresent(typeDesc -> {
            String typeString = typeDesc.toSourceCode().strip();
            constantBuilder.addProperty(PROP_TYPE_DESCRIPTOR, typeString);
        });

        String value = constantDeclarationNode.initializer().toSourceCode().strip();
        constantBuilder.addProperty(PROP_VALUE, value);

        // Extract visibility modifiers and const qualifier
        List<String> modifiers = new ArrayList<>();
        constantDeclarationNode.visibilityQualifier().ifPresent(visibility -> {
            modifiers.add(visibility.text());
        });
        // Extract const keyword dynamically from the syntax tree
        modifiers.add(constantDeclarationNode.constKeyword().text());
        constantBuilder.modifiers(modifiers);

        extractDocumentation(constantDeclarationNode.metadata()).ifPresent(constantBuilder::documentation);
        extractInlineComments(constantDeclarationNode).ifPresent(constantBuilder::comment);

        List<String> annotations = extractAnnotations(constantDeclarationNode.metadata());
        if (!annotations.isEmpty()) {
            constantBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

        return Optional.of(constantBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        CodeMapArtifact.Builder variableBuilder = new CodeMapArtifact.Builder(moduleVariableDeclarationNode)
                .name(CommonUtils.getVariableName(
                        moduleVariableDeclarationNode.typedBindingPattern().bindingPattern()));

        // Extract visibility modifiers and other qualifiers
        List<String> modifiers = new ArrayList<>();
        moduleVariableDeclarationNode.visibilityQualifier().ifPresent(visibility -> {
            modifiers.add(visibility.text());
        });
        modifiers.addAll(extractModifiers(moduleVariableDeclarationNode.qualifiers()));
        variableBuilder.modifiers(modifiers);

        variableBuilder.type(TYPE_VARIABLE);

        // Handle configurable variables differently
        if (hasQualifier(moduleVariableDeclarationNode.qualifiers(), SyntaxKind.CONFIGURABLE_KEYWORD)) {
            TypeDescriptorNode typeDesc = moduleVariableDeclarationNode.typedBindingPattern().typeDescriptor();
            if (typeDesc != null) {
                String typeString = typeDesc.toSourceCode().strip();
                variableBuilder.addProperty(PROP_TYPE_DESCRIPTOR, typeString);
            }
        } else {
            // Check if this is a connection/client variable
            Optional<ClassSymbol> connection = getConnection(moduleVariableDeclarationNode);
            if (connection.isPresent()) {
                variableBuilder
                        .addProperty(PROP_TYPE, connection.get().signature());
                // Special handling for persist clients
                if (isPersistClient(connection.get(), semanticModel)) {
                    variableBuilder.addProperty(CONNECTOR_TYPE, PERSIST);
                    getPersistModelFilePath(projectPath, connection.get())
                            .ifPresent(modelFile -> variableBuilder.addProperty(PERSIST_MODEL_FILE, modelFile));
                }
            }
        }

        // Get type information from semantic model
        semanticModel.symbol(moduleVariableDeclarationNode).ifPresent(symbol -> {
            if (symbol instanceof VariableSymbol variableSymbol) {
                variableBuilder.addProperty(PROP_TYPE,
                        CommonUtils.getTypeSignature(
                                variableSymbol.typeDescriptor(), moduleInfo));
            }
        });

        extractDocumentation(moduleVariableDeclarationNode.metadata()).ifPresent(variableBuilder::documentation);
        extractInlineComments(moduleVariableDeclarationNode).ifPresent(variableBuilder::comment);

        List<String> annotations = extractAnnotations(moduleVariableDeclarationNode.metadata());
        if (!annotations.isEmpty()) {
            variableBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

        return Optional.of(variableBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(TypeDefinitionNode typeDefinitionNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(typeDefinitionNode)
                .name(typeDefinitionNode.typeName().text())
                .type(TYPE_TYPE);

        // Extract visibility modifiers
        typeDefinitionNode.visibilityQualifier().ifPresent(visibility -> {
            typeBuilder.modifiers(List.of(visibility.text()));
        });

        // Extract type descriptor from syntax tree first, fallback to semantic model
        String typeDescriptor = extractTypeDescriptorFromSyntax(typeDefinitionNode);
        if (typeDescriptor.isEmpty()) {
            // Use semantic model to get detailed type information for complex types
            semanticModel.symbol(typeDefinitionNode).ifPresent(symbol -> {
                if (symbol instanceof TypeDefinitionSymbol typeDefSymbol) {
                    TypeSymbol typeSymbol = typeDefSymbol.typeDescriptor();
                    // Special handling for record types
                    String semanticTypeDescriptor = isRecordType(typeSymbol)
                            ? RECORD_TYPE_NAME
                            : CommonUtils.getTypeSignature(typeSymbol, moduleInfo);
                    typeBuilder.addProperty(PROP_TYPE_DESCRIPTOR, semanticTypeDescriptor);
                }
            });
        } else {
            typeBuilder.addProperty(PROP_TYPE_DESCRIPTOR, typeDescriptor);
        }

        // Extract fields for record types
        List<String> fields = extractFieldsFromTypeDefinition(typeDefinitionNode);
        typeBuilder.addProperty(PROP_FIELDS, fields);

        // Extract annotations
        List<String> annotations = extractAnnotations(typeDefinitionNode.metadata());
        if (!annotations.isEmpty()) {
            typeBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

        extractDocumentation(typeDefinitionNode.metadata()).ifPresent(typeBuilder::documentation);
        extractInlineComments(typeDefinitionNode).ifPresent(typeBuilder::comment);

        return Optional.of(typeBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(EnumDeclarationNode enumDeclarationNode) {
        CodeMapArtifact.Builder typeBuilder = new CodeMapArtifact.Builder(enumDeclarationNode)
                .name(enumDeclarationNode.identifier().text())
                .type(TYPE_TYPE);

        typeBuilder.addProperty(PROP_TYPE_DESCRIPTOR, ENUM_TYPE_NAME);

        List<String> members = new ArrayList<>();
        for (Node memberNode : enumDeclarationNode.enumMemberList()) {
            if (memberNode instanceof EnumMemberNode enumMember) {
                members.add(enumMember.identifier().text());
            }
        }
        typeBuilder.addProperty(PROP_FIELDS, members);

        extractDocumentation(enumDeclarationNode.metadata()).ifPresent(typeBuilder::documentation);
        extractInlineComments(enumDeclarationNode).ifPresent(typeBuilder::comment);

        List<String> annotations = extractAnnotations(enumDeclarationNode.metadata());
        if (!annotations.isEmpty()) {
            typeBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }
        return Optional.of(typeBuilder.build());
    }

    @Override
    public Optional<CodeMapArtifact> transform(ClassDefinitionNode classDefinitionNode) {
        NodeList<Token> classTypeQualifiers = classDefinitionNode.classTypeQualifiers();

        CodeMapArtifact.Builder classBuilder = new CodeMapArtifact.Builder(classDefinitionNode)
                .name(classDefinitionNode.className().text())
                .type(TYPE_CLASS)
                .modifiers(extractModifiers(classDefinitionNode.visibilityQualifier(), classTypeQualifiers));

        extractDocumentation(classDefinitionNode.metadata()).ifPresent(classBuilder::documentation);
        extractInlineComments(classDefinitionNode).ifPresent(classBuilder::comment);

        List<String> annotations = extractAnnotations(classDefinitionNode.metadata());
        if (!annotations.isEmpty()) {
            classBuilder.addProperty(PROP_ANNOTATIONS, annotations);
        }

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

    /**
     * Transforms type references (used for object and record type inclusions) into CodeMapArtifact objects.
     * Handles both qualified and simple type references with proper source code extraction.
     */
    @Override
    public Optional<CodeMapArtifact> transform(TypeReferenceNode typeReferenceNode) {
        // Extract the type reference source code (e.g., "*persist:AbstractPersistClient")
        String typeRefSource = safeExtractSourceCode(typeReferenceNode);
        if (typeRefSource.isEmpty()) {
            return Optional.empty();
        }

        // The source code already includes the asterisk prefix for type inclusions
        String inclusionName = typeRefSource;

        CodeMapArtifact.Builder inclusionBuilder = new CodeMapArtifact.Builder(typeReferenceNode)
                .name(inclusionName)
                .type(TYPE_INCLUSION);

        // Store the referenced type (without asterisk) for additional context
        String referencedType = typeRefSource.startsWith("*") ? typeRefSource.substring(1) : typeRefSource;
        inclusionBuilder.addProperty(PROP_TYPE, referencedType);

        // Try to get semantic information about the referenced type
        semanticModel.symbol(typeReferenceNode).ifPresent(symbol -> {
            if (symbol instanceof TypeSymbol typeSymbol) {
                String typeSignature = CommonUtils.getTypeSignature(typeSymbol, moduleInfo);
                inclusionBuilder.addProperty(PROP_TYPE_DESCRIPTOR, typeSignature);
            }
        });

        extractInlineComments(typeReferenceNode).ifPresent(inclusionBuilder::comment);

        return Optional.of(inclusionBuilder.build());
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
        if (functionSignature == null) {
            return parameters;
        }

        SeparatedNodeList<ParameterNode> parameterNodes = functionSignature.parameters();

        // Process each parameter type with appropriate formatting
        for (ParameterNode paramNode : parameterNodes) {
            if (paramNode == null) {
                continue;
            }

            try {
                if (paramNode instanceof RequiredParameterNode requiredParam) {
                    // Required parameters: "type name"
                    String fullParamSource = safeExtractSourceCode(requiredParam);
                    if (!fullParamSource.isEmpty()) {
                        parameters.add(fullParamSource);
                    } else {
                        String paramType = safeExtractSourceCode(requiredParam.typeName());
                        String paramName = requiredParam.paramName().map(name -> name.text()).orElse("");
                        if (!paramType.isEmpty()) {
                            parameters.add(paramType + " " + paramName);
                        }
                    }
                } else if (paramNode instanceof DefaultableParameterNode defaultableParam) {
                    // Defaultable parameters: "type name = defaultValue"
                    String fullParamSource = safeExtractSourceCode(defaultableParam);
                    if (!fullParamSource.isEmpty()) {
                        parameters.add(fullParamSource);
                    } else {
                        String paramType = safeExtractSourceCode(defaultableParam.typeName());
                        String paramName = defaultableParam.paramName().map(name -> name.text()).orElse("");
                        String defaultValue = safeExtractSourceCode(defaultableParam.expression());
                        if (!paramType.isEmpty()) {
                            parameters.add(paramType + " " + paramName + " = " + defaultValue);
                        }
                    }
                } else if (paramNode instanceof RestParameterNode restParam) {
                    // Rest parameters: "type... name"
                    String fullParamSource = safeExtractSourceCode(restParam);
                    if (!fullParamSource.isEmpty()) {
                        parameters.add(fullParamSource);
                    } else {
                        String paramType = safeExtractSourceCode(restParam.typeName());
                        String paramName = restParam.paramName().map(name -> name.text()).orElse("");
                        if (!paramType.isEmpty()) {
                            parameters.add(paramType + "... " + paramName);
                        }
                    }
                } else {
                    // Fallback for other parameter types
                    String paramSource = safeExtractSourceCode(paramNode);
                    if (!paramSource.isEmpty()) {
                        parameters.add(paramSource);
                    }
                }
            } catch (RuntimeException e) {
                continue;
            }
        }
        return parameters;
    }

    private String safeExtractSourceCode(Node node) {
        if (node == null) {
            return "";
        }
        try {
            String sourceCode = node.toSourceCode();
            return normalizeWhitespace(sourceCode != null ? sourceCode : "");
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String extractReturnType(FunctionSignatureNode functionSignature) {
        return functionSignature.returnTypeDesc()
                .map(returnTypeDesc -> {
                    // Use the full return type descriptor which includes annotations
                    String fullReturnType = safeExtractSourceCode(returnTypeDesc);
                    if (!fullReturnType.isEmpty()) {
                        // Remove the "returns" keyword since the markdown generator will add it back
                        return fullReturnType.replaceFirst("^\\s*returns\\s+", "").strip();
                    } else {
                        return returnTypeDesc.type().toSourceCode().strip();
                    }
                })
                .orElse("()");
    }

    /**
     * Extracts type descriptor directly from the syntax tree for simple type aliases.
     * This preserves the original source code without module prefixes added by the semantic model.
     * For record types, returns just "record" to avoid cluttering with field details.
     */
    private String extractTypeDescriptorFromSyntax(TypeDefinitionNode typeDefinitionNode) {
        Node typeDescriptor = typeDefinitionNode.typeDescriptor();
        if (typeDescriptor != null) {
            String sourceCode = safeExtractSourceCode(typeDescriptor);

            // Strip field definitions from record types
            if (sourceCode.contains("record {|") && sourceCode.contains("|}")) {
                int recordStart = sourceCode.indexOf("record {|");
                int recordEnd = sourceCode.indexOf("|}", recordStart);
                if (recordStart != -1 && recordEnd != -1) {
                    String prefix = sourceCode.substring(0, recordStart);
                    String suffix = sourceCode.substring(recordEnd + 2);
                    return (prefix + RECORD_TYPE_NAME + suffix).trim();
                }
            }

            if (sourceCode.startsWith("record {") || sourceCode.startsWith("record{")) {
                return RECORD_TYPE_NAME;
            }
            return sourceCode;
        }
        return "";
    }

    private List<String> extractFieldsFromTypeDefinition(TypeDefinitionNode typeDefinitionNode) {
        List<String> fields = new ArrayList<>();
        // Extract record fields using semantic model
        semanticModel.symbol(typeDefinitionNode).ifPresent(symbol -> {
            if (symbol instanceof TypeDefinitionSymbol typeDefSymbol) {
                TypeSymbol typeSymbol = typeDefSymbol.typeDescriptor();
                RecordTypeSymbol recordType = getRecordTypeSymbol(typeSymbol);
                if (recordType != null) {
                    // Format each field as "fieldName: fieldType"
                    for (RecordFieldSymbol field : recordType.fieldDescriptors().values()) {
                        fields.add(field.getName().orElse("") + ": " +
                                CommonUtils.getTypeSignature(
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
        // Direct record type
        if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
            return (RecordTypeSymbol) typeSymbol;
        }
        // Record wrapped in intersection type (e.g., record & readonly)
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
            // For services without type descriptor, use just the path as service type (empty service name)
            return Optional.empty();
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
                    .map(symbol -> CommonUtils
                            .getTypeSignature((TypeSymbol) symbol, moduleInfo));
        }

        if (expression instanceof ImplicitNewExpressionNode) {
            return semanticModel.typeOf(expression)
                    .map(typeSymbol -> CommonUtils
                            .getTypeSignature(typeSymbol, moduleInfo));
        }
        return semanticModel.symbol(expression)
                .filter(symbol -> symbol instanceof VariableSymbol)
                .map(symbol -> ((VariableSymbol) symbol).typeDescriptor())
                .map(typeSymbol -> CommonUtils
                        .getTypeSignature(typeSymbol, moduleInfo));
    }

    private Optional<ClassSymbol> getConnection(Node node) {
        // Check if this variable represents a connection/client
        try {
            Optional<Symbol> symbolOpt = semanticModel.symbol(node);
            if (symbolOpt.isEmpty()) {
                return Optional.empty();
            }

            Symbol symbol = symbolOpt.get();
            if (!(symbol instanceof VariableSymbol variableSymbol)) {
                return Optional.empty();
            }

            // Navigate through type reference to get actual class symbol
            TypeSymbol typeDescriptor = variableSymbol.typeDescriptor();
            if (!(typeDescriptor instanceof TypeReferenceTypeSymbol typeRefSymbol)) {
                return Optional.empty();
            }

            TypeSymbol actualType = typeRefSymbol.typeDescriptor();
            if (!(actualType instanceof ClassSymbol classSymbol)) {
                return Optional.empty();
            }

            // Check if it's a client or AI-related connection
            if (classSymbol.qualifiers().contains(Qualifier.CLIENT) || isAiKnowledgeBase(classSymbol)
                    || isAiVectorStore(symbol) || isAiMemoryStore(symbol)) {
                return Optional.of(classSymbol);
            }
        } catch (ClassCastException e) {
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
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
                    boolean firstLine = true;

                    for (Node documentationLine : docNode.documentationLines()) {
                        SyntaxKind lineKind = documentationLine.kind();

                        if (lineKind == SyntaxKind.MARKDOWN_DOCUMENTATION_LINE ||
                                lineKind == SyntaxKind.MARKDOWN_REFERENCE_DOCUMENTATION_LINE ||
                                lineKind == SyntaxKind.MARKDOWN_DEPRECATION_DOCUMENTATION_LINE ||
                                lineKind == SyntaxKind.MARKDOWN_PARAMETER_DOCUMENTATION_LINE ||
                                lineKind == SyntaxKind.MARKDOWN_RETURN_PARAMETER_DOCUMENTATION_LINE) {

                            if (!firstLine) {
                                description.append('\n');
                            }
                            firstLine = false;

                            StringBuilder lineContent = new StringBuilder();
                            if (documentationLine instanceof MarkdownDocumentationLineNode) {
                                NodeList<Node> elements = ((MarkdownDocumentationLineNode) documentationLine)
                                        .documentElements();
                                elements.forEach(element -> lineContent.append(element.toSourceCode()));
                            } else {
                                lineContent.append(documentationLine.toSourceCode());
                            }
                            String line = lineContent.toString();
                            description.append(line);
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
        node.leadingMinutiae().forEach(minutiae -> {
            if (minutiae.kind() == SyntaxKind.COMMENT_MINUTIAE) {
                String commentText = minutiae.text().strip();
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

    private List<String> extractAnnotations(Optional<MetadataNode> metadata) {
        if (metadata.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> annotations = new ArrayList<>();
        for (AnnotationNode annotation : metadata.get().annotations()) {
            StringBuilder annotationStr = new StringBuilder("@");

            Node annotReference = annotation.annotReference();
            if (annotReference.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                QualifiedNameReferenceNode qNameRef = (QualifiedNameReferenceNode) annotReference;
                String prefix = qNameRef.modulePrefix().text();
                String identifier = qNameRef.identifier().text();
                annotationStr.append(prefix).append(":").append(identifier);
            } else if (annotReference.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                SimpleNameReferenceNode simpleRef = (SimpleNameReferenceNode) annotReference;
                annotationStr.append(simpleRef.name().text());
            } else {
                annotationStr.append(annotReference.toSourceCode().strip());
            }

            Optional<MappingConstructorExpressionNode> annotValue = annotation.annotValue();
            if (annotValue.isPresent()) {
                annotationStr.append(" {").append(extractAnnotationValue(annotValue.get())).append("}");
            }

            annotations.add(annotationStr.toString());
        }
        return annotations;
    }

    private String extractAnnotationValue(MappingConstructorExpressionNode mappingNode) {
        List<String> fields = new ArrayList<>();

        for (MappingFieldNode field : mappingNode.fields()) {
            if (field instanceof SpecificFieldNode specificField) {
                String fieldName = specificField.fieldName().toSourceCode().strip();
                Optional<ExpressionNode> valueExpr = specificField.valueExpr();

                if (valueExpr.isPresent()) {
                    String value = extractExpressionValue(valueExpr.get());
                    fields.add(fieldName + ": " + value);
                }
            } else {
                fields.add(field.toSourceCode().strip());
            }
        }

        return String.join(", ", fields);
    }

    private String extractExpressionValue(ExpressionNode expression) {
        if (expression.kind() == SyntaxKind.STRING_LITERAL) {
            BasicLiteralNode literalNode = (BasicLiteralNode) expression;
            String value = literalNode.literalToken().text();
            return value;
        } else if (expression.kind() == SyntaxKind.BOOLEAN_LITERAL ||
                expression.kind() == SyntaxKind.NUMERIC_LITERAL) {
            BasicLiteralNode literalNode = (BasicLiteralNode) expression;
            return literalNode.literalToken().text();
        } else {
            return expression.toSourceCode().strip();
        }
    }

}

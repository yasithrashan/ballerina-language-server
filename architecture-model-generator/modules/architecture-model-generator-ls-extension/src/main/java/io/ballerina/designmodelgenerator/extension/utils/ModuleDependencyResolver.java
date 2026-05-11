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

package io.ballerina.designmodelgenerator.extension.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.designmodelgenerator.extension.response.CodeMapResolveModuleDependenciesResponse;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import org.ballerinalang.langserver.command.executors.PullModuleExecutor;
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.exception.UserErrorException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Utility class for resolving module dependencies in Ballerina projects and workspaces.
 * This class provides functionality to detect unresolved module dependencies (BCE2003 errors)
 * and automatically resolve them by pulling required modules from Ballerina Central.
 * Supports both single packages and multi-package workspaces.
 *
 * @since 1.6.0
 */
public class ModuleDependencyResolver {

    private static final String UNRESOLVED_MODULE_CODE = "BCE2003";
    private static final String RESOLVE_MODULE_FAILURE_MESSAGE =
            "An internal error occurred while resolving module dependencies.";

    /**
     * Checks if there are unresolved modules in a workspace.
     *
     * @param project the root project
     * @param workspaceManager the workspace manager
     * @param compilerApi the compiler API instance
     * @return true if there are unresolved modules in the workspace
     */
    public static boolean hasUnresolvedModulesInWorkspace(Project project, WorkspaceManager workspaceManager,
                                                          BallerinaCompilerApi compilerApi) {
        List<Project> workspaceProjects = compilerApi.getWorkspaceProjectsInOrder(project);

        for (Project packageProject : workspaceProjects) {
            if (hasUnresolvedModulesInPackage(packageProject, workspaceManager)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there are unresolved modules in a package.
     *
     * @param project the project
     * @param workspaceManager the workspace manager
     * @return true if there are unresolved modules in the package
     */
    public static boolean hasUnresolvedModulesInPackage(Project project, WorkspaceManager workspaceManager) {
        Package currentPackage = project.currentPackage();

        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);

            // Get semantic model for the entire module instead of per document
            Optional<SemanticModel> semanticModel = getModuleSemanticModel(project, module);

            if (semanticModel.isPresent() && hasUnresolvedModules(semanticModel.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the semantic model for a specific module from the project's package compilation.
     * This provides access to the module's type information and diagnostics.
     *
     * @param project the project containing the module
     * @param module the module to get the semantic model for
     * @return the semantic model for the module, or empty if compilation fails
     */
    public static Optional<SemanticModel> getModuleSemanticModel(Project project, Module module) {
        try {
            PackageCompilation packageCompilation = project.currentPackage().getCompilation();
            SemanticModel semanticModel = packageCompilation.getSemanticModel(module.moduleId());
            return Optional.of(semanticModel);
        } catch (Exception e) {
            // Return empty if compilation fails due to syntax errors or missing dependencies
            return Optional.empty();
        }
    }

    /**
     * Checks if a semantic model has unresolved modules by looking for BCE2003 diagnostics.
     * BCE2003 indicates that a required module could not be found or loaded.
     *
     * @param semanticModel the semantic model to check for unresolved module diagnostics
     * @return true if there are unresolved modules (BCE2003 errors present)
     */
    public static boolean hasUnresolvedModules(SemanticModel semanticModel) {
        return semanticModel.diagnostics().stream()
                .anyMatch(diagnostic -> UNRESOLVED_MODULE_CODE.equals(diagnostic.diagnosticInfo().code()));
    }

    /**
     * Gets the file system URI for a specific module's root directory.
     * For single-file projects, returns the file URI. For default modules, returns
     * the project source root. For sub-modules, returns the modules/moduleName directory.
     *
     * @param project the project containing the module
     * @param module the module to get the URI for
     * @return the module's root directory URI as a string
     */
    public static String getModuleUri(Project project, Module module) {
        Path sourceRoot = project.sourceRoot();
        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT) {
            return sourceRoot.toUri().toString();
        }
        if (module.isDefaultModule()) {
            return sourceRoot.toUri().toString();
        }
        return sourceRoot.resolve("modules").resolve(module.moduleName().moduleNamePart()).toUri().toString();
    }

    /**
     * Executes module resolution for a single project by pulling missing dependencies.
     * Uses any module URI from the package since all modules share the same package dependencies.
     *
     * @param project the project to resolve dependencies for
     * @param workspaceManager the workspace manager for project access
     * @param serverContext the language server context containing the client
     * @throws ExecutionException if the pull module operation fails
     * @throws InterruptedException if the operation is interrupted
     */
    public static void executeResolveModulesForProject(Project project, WorkspaceManager workspaceManager,
                                                     LanguageServerContext serverContext)
            throws ExecutionException, InterruptedException {
        Package currentPackage = project.currentPackage();

        // Get any module URI from the package - they all resolve the same package dependencies
        if (!currentPackage.moduleIds().isEmpty()) {
            ModuleId firstModuleId = currentPackage.moduleIds().iterator().next();
            Module firstModule = currentPackage.module(firstModuleId);
            String moduleUri = getModuleUri(project, firstModule);

            PullModuleExecutor.resolveModules(
                    moduleUri,
                    serverContext.get(ExtendedLanguageClient.class),
                    workspaceManager,
                    serverContext
            ).get();
        }
    }

    /**
     * Executes module resolution for all packages in a multi-package workspace.
     * Iterates through all workspace projects and resolves dependencies for each.
     *
     * @param project the root workspace project
     * @param workspaceManager the workspace manager for project access
     * @param serverContext the language server context containing the client
     * @param compilerApi the compiler API instance for workspace operations
     * @throws ExecutionException if any package's dependency resolution fails
     * @throws InterruptedException if the operation is interrupted
     */
    public static void executeResolveModulesForWorkspace(Project project, WorkspaceManager workspaceManager,
                                                        LanguageServerContext serverContext,
                                                        BallerinaCompilerApi compilerApi)
            throws ExecutionException, InterruptedException {
        List<Project> workspaceProjects = compilerApi.getWorkspaceProjectsInOrder(project);

        for (Project packageProject : workspaceProjects) {
            executeResolveModulesForProject(packageProject, workspaceManager, serverContext);
        }
    }

    /**
     * Handles exceptions during module resolution and sets appropriate response values.
     * Extracts user-friendly error messages from UserErrorExceptions, otherwise uses a generic message.
     *
     * @param response the response object to update with error information
     * @param e the exception that occurred during module resolution
     */
    public static void handleException(CodeMapResolveModuleDependenciesResponse response, Throwable e) {
        response.setSuccess(false);
        response.setErrorMsg(e instanceof UserErrorException ? e.getMessage() :
                e.getCause() instanceof UserErrorException ? e.getCause().getMessage() :
                RESOLVE_MODULE_FAILURE_MESSAGE);
    }
}

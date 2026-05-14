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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private static final String RESOLVE_MODULE_TIMEOUT_MESSAGE =
            "Module dependency resolution timed out. Please try again or check your network connection.";
    private static final long MODULE_RESOLUTION_TIMEOUT_MINUTES = 3;

    /**
     * Finds all packages with unresolved module imports across a project or workspace.
     * Walks each package's modules and short-circuits per-package on the first BCE2003 diagnostic,
     * so a package with one missing import does not trigger a full scan of its remaining modules.
     *
     * @param project the root project (single package or workspace)
     * @param compilerApi the compiler API instance
     * @return the packages containing unresolved modules; empty if everything resolves
     */
    public static List<Project> findUnresolvedPackages(Project project, BallerinaCompilerApi compilerApi) {
        List<Project> candidates = compilerApi.isWorkspaceProject(project)
                ? compilerApi.getWorkspaceProjectsInOrder(project)
                : List.of(project);

        List<Project> unresolved = new ArrayList<>();

        for (Project packageProject : candidates) {
            if (hasUnresolvedModulesInPackage(packageProject)) {
                unresolved.add(packageProject);
            }
        }
        return unresolved;
    }

    private static boolean hasUnresolvedModulesInPackage(Project project) {
        Package currentPackage = project.currentPackage();
        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);
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
     * Resolves dependencies for a project by pulling missing modules.
     *
     * @param project the project to resolve dependencies for
     * @param workspaceManager the workspace manager
     * @param serverContext the language server context
     * @throws ExecutionException if resolution fails
     * @throws InterruptedException if interrupted
     * @throws TimeoutException if resolution times out
     */
    public static void executeResolveModulesForProject(Project project, WorkspaceManager workspaceManager,
                                                     LanguageServerContext serverContext)
            throws ExecutionException, InterruptedException, TimeoutException {
        Package currentPackage = project.currentPackage();

        // Use default module URI for dependency resolution
        Module defaultModule = currentPackage.getDefaultModule();
        String moduleUri = getModuleUri(project, defaultModule);

        PullModuleExecutor.resolveModules(
                moduleUri,
                serverContext.get(ExtendedLanguageClient.class),
                workspaceManager,
                serverContext
        ).get(MODULE_RESOLUTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Resolves dependencies for multiple packages.
     *
     * @param packages packages with unresolved dependencies
     * @param workspaceManager the workspace manager
     * @param serverContext the language server context
     * @throws ExecutionException if resolution fails
     * @throws InterruptedException if interrupted
     * @throws TimeoutException if resolution times out
     */
    public static void resolvePackages(List<Project> packages, WorkspaceManager workspaceManager,
                                       LanguageServerContext serverContext)
            throws ExecutionException, InterruptedException, TimeoutException {
        for (Project packageProject : packages) {
            executeResolveModulesForProject(packageProject, workspaceManager, serverContext);
        }
    }

    /**
     * Handles exceptions during dependency resolution.
     *
     * @param response the response to update
     * @param e the exception that occurred
     */
    public static void handleException(CodeMapResolveModuleDependenciesResponse response, Throwable e) {
        response.setSuccess(false);
        // Extract user-friendly error messages from different exception types
        if (e instanceof TimeoutException || e.getCause() instanceof TimeoutException) {
            response.setErrorMsg(RESOLVE_MODULE_TIMEOUT_MESSAGE);
        } else if (e instanceof UserErrorException) {
            response.setErrorMsg(e.getMessage());
        } else if (e.getCause() instanceof UserErrorException) {
            response.setErrorMsg(e.getCause().getMessage());
        } else {
            response.setErrorMsg(RESOLVE_MODULE_FAILURE_MESSAGE);
        }
    }
}

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
    private static final long MODULE_RESOLUTION_TIMEOUT_SECONDS = 13;

    private ModuleDependencyResolver() {
    }

    private record FailedPackage(String name, Exception cause) {
    }

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
        PackageCompilation compilation;
        try {
            compilation = currentPackage.getCompilation();
        } catch (RuntimeException e) {
            return false;
        }
        for (ModuleId moduleId : currentPackage.moduleIds()) {
            if (hasUnresolvedModules(compilation.getSemanticModel(moduleId))) {
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
        Module defaultModule = currentPackage.getDefaultModule();
        String moduleUri = getModuleUri(project, defaultModule);

        PullModuleExecutor.resolveModules(
                moduleUri,
                serverContext.get(ExtendedLanguageClient.class),
                workspaceManager,
                serverContext
        ).get(MODULE_RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Resolves dependencies for multiple packages.
     * Continues processing remaining packages even if some fail or timeout.
     *
     * @param rootProject the root project (workspace or single package) used to format failure names
     * @param packages packages with unresolved dependencies
     * @param workspaceManager the workspace manager
     * @param serverContext the language server context
     * @throws ExecutionException if any packages fail to resolve
     * @throws InterruptedException if interrupted
     * @throws TimeoutException if any packages timeout
     */
    public static void resolvePackages(Project rootProject, List<Project> packages, WorkspaceManager workspaceManager,
                                       LanguageServerContext serverContext)
            throws ExecutionException, InterruptedException, TimeoutException {
        List<FailedPackage> failed = new ArrayList<>();

        for (Project packageProject : packages) {
            try {
                executeResolveModulesForProject(packageProject, workspaceManager, serverContext);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                failed.add(new FailedPackage(getPackageDisplayName(rootProject, packageProject), e));
            }
        }

        if (failed.isEmpty()) {
            return;
        }

        StringBuilder details = new StringBuilder();
        for (int i = 0; i < failed.size(); i++) {
            FailedPackage fp = failed.get(i);
            details.append(fp.name()).append(" - ").append(getErrorMessage(fp.cause()));
            if (i < failed.size() - 1) {
                details.append("\n");
            }
        }

        String summary = "Failed to resolve " + formatPackageList(failed);
        PackageResolutionException ex = new PackageResolutionException(
                summary, details.toString(), failed.getFirst().cause());
        for (int i = 1; i < failed.size(); i++) {
            ex.addSuppressed(failed.get(i).cause());
        }
        throw ex;
    }

    /**
     * Builds a display name for a failed package.
     * For workspace projects, the format is {@code <workspace-dir>.<package-name>} since the workspace
     * root has no Ballerina.toml. For single packages, the Ballerina.toml name is used, falling back to
     * the source root directory name if unavailable.
     *
     * @param rootProject the root project (workspace or single package)
     * @param packageProject the package whose display name is being computed
     * @return the formatted display name
     */
    private static String getPackageDisplayName(Project rootProject, Project packageProject) {
        String packageName = packageProject.currentPackage().packageName().value();
        String fallback = getDirectoryName(packageProject.sourceRoot());
        String resolved = (packageName == null || packageName.isBlank()) ? fallback : packageName;

        if (BallerinaCompilerApi.getInstance().isWorkspaceProject(rootProject)) {
            String workspaceName = getDirectoryName(rootProject.sourceRoot());
            return workspaceName + "." + resolved;
        }
        return resolved;
    }

    private static String getDirectoryName(Path path) {
        Path name = path.getFileName();
        return name != null ? name.toString() : path.toString();
    }

    private static String formatPackageList(List<FailedPackage> failed) {
        if (failed.size() == 1) {
            return failed.getFirst().name() + " package";
        }
        List<String> names = failed.stream().map(FailedPackage::name).toList();
        String joined = names.size() == 2
                ? names.get(0) + " and " + names.get(1)
                : String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
        return joined + " packages";
    }


    /**
     * Gets the error message from an exception, extracting meaningful details.
     *
     * @param exception the exception to analyze
     * @return the error message from the exception
     */
    private static String getErrorMessage(Exception exception) {
        return switch (exception) {
            case TimeoutException ignored -> RESOLVE_MODULE_TIMEOUT_MESSAGE;
            case InterruptedException ignored -> "Module dependency resolution was interrupted";
            case ExecutionException ex -> {
                Throwable cause = ex.getCause();
                if (cause instanceof TimeoutException) {
                    yield RESOLVE_MODULE_TIMEOUT_MESSAGE;
                }
                if (cause != null && cause.getMessage() != null) {
                    yield cause.getMessage();
                }
                yield ex.getMessage() != null ? ex.getMessage() : "Execution failed";
            }
            default -> exception.getMessage() != null ? exception.getMessage() : "Unknown error occurred";
        };
    }

    /**
     * Handles exceptions during dependency resolution.
     *
     * @param response the response to update
     * @param e the exception that occurred
     */
    public static void handleException(CodeMapResolveModuleDependenciesResponse response, Throwable e) {
        response.setSuccess(false);

        if (e instanceof PackageResolutionException pe) {
            response.setErrorMsg(pe.getMessage());
            response.setErrorDetails(pe.getErrorDetails());
            return;
        }

        if (e instanceof TimeoutException) {
            String msg = e.getMessage();
            response.setErrorMsg(hasFailedToResolveMessage(msg) ? msg : RESOLVE_MODULE_TIMEOUT_MESSAGE);
            return;
        }

        if (e.getCause() instanceof TimeoutException) {
            response.setErrorMsg(RESOLVE_MODULE_TIMEOUT_MESSAGE);
            return;
        }

        if (e instanceof UserErrorException) {
            response.setErrorMsg(e.getMessage());
            return;
        }

        if (e.getCause() instanceof UserErrorException ue) {
            response.setErrorMsg(ue.getMessage());
            return;
        }

        response.setErrorMsg(RESOLVE_MODULE_FAILURE_MESSAGE);
    }

    private static boolean hasFailedToResolveMessage(String msg) {
        return msg != null
                && (msg.contains("packages failed to resolve") || msg.contains("package failed to resolve"));
    }
}

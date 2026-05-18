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

package io.ballerina.designmodelgenerator.extension.utils.codemapresolvemodules;

import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import org.ballerinalang.langserver.command.executors.PullModuleExecutor;
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Pulls missing modules from Ballerina Central via {@link PullModuleExecutor} and aggregates
 * per-package failures into a {@link PackageResolutionException}. Continues processing remaining
 * packages even if some fail or timeout.
 *
 * @since 1.6.0
 */
public class ModulePuller {

    private static final long MODULE_RESOLUTION_TIMEOUT_SECONDS = 120;

    private ModulePuller() {
    }

    private record FailedPackage(String name, Exception cause) {
    }

    /**
     * Resolves dependencies for multiple packages.
     * Continues processing remaining packages even if some fail or timeout.
     *
     * @param rootProject the root project (workspace or single package) used to format failure names
     * @param packages packages with unresolved dependencies
     * @param workspaceManager the workspace manager
     * @param serverContext the language server context
     */
    public static void resolvePackages(Project rootProject, List<Project> packages, WorkspaceManager workspaceManager,
                                       LanguageServerContext serverContext) {
        List<FailedPackage> failed = new ArrayList<>();

        for (Project packageProject : packages) {
            try {
                executeResolveModulesForProject(packageProject, workspaceManager, serverContext);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failed.add(new FailedPackage(getPackageDisplayName(rootProject, packageProject), e));
                break;
            } catch (ExecutionException | TimeoutException e) {
                failed.add(new FailedPackage(getPackageDisplayName(rootProject, packageProject), e));
            }
        }

        if (failed.isEmpty()) {
            return;
        }

        String details = failed.stream()
                .map(fp -> fp.name() + " - " + getErrorMessage(fp.cause()))
                .collect(Collectors.joining("\n"));

        String summary = "Failed to resolve " + formatPackageList(failed);
        PackageResolutionException ex = new PackageResolutionException(
                summary, details, failed.getFirst().cause());
        for (int i = 1; i < failed.size(); i++) {
            ex.addSuppressed(failed.get(i).cause());
        }
        throw ex;
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
        if (project.kind() == ProjectKind.SINGLE_FILE_PROJECT || module.isDefaultModule()) {
            return sourceRoot.toUri().toString();
        }
        return sourceRoot.resolve("modules").resolve(module.moduleName().moduleNamePart()).toUri().toString();
    }

    /**
     * Builds a display name for a failed package.
     * For workspace projects, the format is {@code <workspace-dir>.<package-name>} since the workspace
     * root has no Ballerina.toml. For single packages, the Ballerina.toml name is used, falling back to
     * the source root directory name if unavailable.
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

    private static String getErrorMessage(Exception exception) {
        return switch (exception) {
            case TimeoutException te -> ResolveExceptionHandler.RESOLVE_MODULE_TIMEOUT_MESSAGE;
            case InterruptedException ie -> "Module dependency resolution was interrupted";
            case ExecutionException ex -> {
                Throwable cause = ex.getCause();
                if (cause instanceof TimeoutException) {
                    yield ResolveExceptionHandler.RESOLVE_MODULE_TIMEOUT_MESSAGE;
                }
                if (cause != null && cause.getMessage() != null) {
                    yield cause.getMessage();
                }
                yield ex.getMessage() != null ? ex.getMessage() : "Execution failed";
            }
            default -> exception.getMessage() != null ? exception.getMessage() : "Unknown error occurred";
        };
    }
}

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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.BallerinaCompilerApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects packages with unresolved module imports (BCE2003 diagnostics) across
 * a single package or workspace project.
 *
 * @since 1.6.0
 */
public class UnresolvedModuleChecker {

    private static final String UNRESOLVED_MODULE_CODE = "BCE2003";

    private UnresolvedModuleChecker() {
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

    private static boolean hasUnresolvedModules(SemanticModel semanticModel) {
        return semanticModel.diagnostics().stream()
                .anyMatch(diagnostic -> UNRESOLVED_MODULE_CODE.equals(diagnostic.diagnosticInfo().code()));
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
}

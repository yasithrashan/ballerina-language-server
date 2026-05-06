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

package io.ballerina.designmodelgenerator.extension.response;

/**
 * Represents the response for codemap module dependency resolution operations.
 * Similar to ResolveModuleDependenciesResponse but with support for sub-modules and workspaces.
 * <p>
 * The {@code success} field indicates whether all the modules (including sub-modules)
 * in the package/workspace are successfully resolved.
 * This is {@code true} when all the modules are either already resolved,
 * or after successfully resolving the unresolved modules; otherwise, {@code false}.
 * <p>
 * The {@code errorMsg} field contains the error message describing the failure.
 * This is set only when {@code success} is {@code false}.
 *
 * @since 1.6.0
 */
public class CodeMapResolveModuleDependenciesResponse {
    private boolean success;
    private String errorMsg;

    public CodeMapResolveModuleDependenciesResponse() {
    }

    public CodeMapResolveModuleDependenciesResponse(boolean success, String errorMsg) {
        this.success = success;
        this.errorMsg = errorMsg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}

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

import io.ballerina.designmodelgenerator.extension.response.CodeMapResolveModuleDependenciesResponse;
import org.ballerinalang.langserver.exception.UserErrorException;

import java.util.concurrent.TimeoutException;

/**
 * Translates exceptions thrown during module dependency resolution into the
 * {@link CodeMapResolveModuleDependenciesResponse} fields.
 *
 * @since 1.6.0
 */
public class ModuleResolutionHandler {

    static final String RESOLVE_MODULE_FAILURE_MESSAGE =
            "An internal error occurred while resolving module dependencies.";
    static final String RESOLVE_MODULE_TIMEOUT_MESSAGE =
            "Module dependency resolution timed out. Please try again or check your network connection.";

    private ModuleResolutionHandler() {
    }

    /**
     * Handles exceptions during dependency resolution.
     *
     * @param response the response to update
     * @param e        the exception that occurred
     */
    public static void handleException(CodeMapResolveModuleDependenciesResponse response, Throwable e) {
        response.setSuccess(false);

        if (e instanceof PackageResolutionException pe) {
            response.setErrorMsg(pe.getMessage());
            response.setErrorDetails(pe.getErrorDetails());
            return;
        }

        if (e instanceof TimeoutException) {
            response.setErrorMsg(RESOLVE_MODULE_TIMEOUT_MESSAGE);
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
}

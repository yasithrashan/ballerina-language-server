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

package io.ballerina.designmodelgenerator.extension;

import io.ballerina.designmodelgenerator.extension.request.CodeMapResolveModuleDependenciesRequest;
import io.ballerina.designmodelgenerator.extension.response.CodeMapResolveModuleDependenciesResponse;
import io.ballerina.designmodelgenerator.extension.utils.codemapresolvemodules.ResolveExceptionHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Tests for the codeMapResolveModuleDependencies API and related utility methods.
 *
 * @since 1.6.0
 */
public class CodeMapResolveModuleDependenciesTest {

    @Test
    public void testCodeMapResolveModuleDependenciesRequestCreation() {
        String testPath = "/test/path";
        CodeMapResolveModuleDependenciesRequest request = new CodeMapResolveModuleDependenciesRequest(testPath);

        Assert.assertNotNull(request);
        Assert.assertEquals(request.projectPath(), testPath);
    }

    @Test
    public void testCodeMapResolveModuleDependenciesResponseCreation() {
        CodeMapResolveModuleDependenciesResponse response = new CodeMapResolveModuleDependenciesResponse();
        response.setSuccess(false);
        response.setErrorMsg("Test error message");

        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(response.getErrorMsg(), "Test error message");
    }

    @Test
    public void testCodeMapResolveModuleDependenciesResponseWithConstructor() {
        boolean success = true;
        String errorMsg = "No error";
        CodeMapResolveModuleDependenciesResponse response =
                new CodeMapResolveModuleDependenciesResponse(success, errorMsg);

        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(response.getErrorMsg(), errorMsg);
    }

    @Test
    public void testModuleDependencyResolverHandleException() {
        CodeMapResolveModuleDependenciesResponse response = new CodeMapResolveModuleDependenciesResponse();
        RuntimeException testException = new RuntimeException("Test exception");

        ResolveExceptionHandler.handleException(response, testException);

        Assert.assertFalse(response.isSuccess());
        Assert.assertNotNull(response.getErrorMsg());
        Assert.assertEquals(response.getErrorMsg(),
                "An internal error occurred while resolving module dependencies.");
    }

    @Test
    public void testModuleDependencyResolverHandleTimeoutException() {
        CodeMapResolveModuleDependenciesResponse response = new CodeMapResolveModuleDependenciesResponse();
        java.util.concurrent.TimeoutException timeoutException =
                new java.util.concurrent.TimeoutException("Timeout occurred");

        ResolveExceptionHandler.handleException(response, timeoutException);

        Assert.assertFalse(response.isSuccess());
        Assert.assertNotNull(response.getErrorMsg());
        Assert.assertEquals(response.getErrorMsg(),
                "Module dependency resolution timed out. Please try again or check your network connection.");
    }

    @Test
    public void testCodeMapResolveModuleDependenciesWithInvalidPath() throws Exception {
        DesignModelGeneratorService service = new DesignModelGeneratorService();
        String invalidProjectPath = "/invalid/path/to/project";
        CodeMapResolveModuleDependenciesRequest request =
                new CodeMapResolveModuleDependenciesRequest(invalidProjectPath);

        CompletableFuture<CodeMapResolveModuleDependenciesResponse> future =
                service.codeMapResolveModuleDependencies(request);
        CodeMapResolveModuleDependenciesResponse response = future.get();

        Assert.assertFalse(response.isSuccess(), "Expected failure response for invalid project path");
        Assert.assertNotNull(response.getErrorMsg(), "Error message should not be null for error response");
        Assert.assertTrue(response.getErrorMsg().contains("internal error"),
                "Error message should indicate internal error");
    }

    @Test
    public void testModuleDependencyResolverHandleExceptionWithDetailedMessage() {
        CodeMapResolveModuleDependenciesResponse response = new CodeMapResolveModuleDependenciesResponse();
        java.util.concurrent.TimeoutException timeoutException =
                new java.util.concurrent.TimeoutException("2 of 3 packages failed to resolve: packageA, packageB");

        ResolveExceptionHandler.handleException(response, timeoutException);

        Assert.assertFalse(response.isSuccess());
        Assert.assertNotNull(response.getErrorMsg());
        Assert.assertEquals(response.getErrorMsg(),
                "2 of 3 packages failed to resolve: packageA, packageB");
    }

    @Test
    public void testModuleDependencyResolverHandleExceptionWithGenericTimeoutMessage() {
        CodeMapResolveModuleDependenciesResponse response = new CodeMapResolveModuleDependenciesResponse();
        java.util.concurrent.TimeoutException timeoutException =
                new java.util.concurrent.TimeoutException("Generic timeout");

        ResolveExceptionHandler.handleException(response, timeoutException);

        Assert.assertFalse(response.isSuccess());
        Assert.assertNotNull(response.getErrorMsg());
        Assert.assertEquals(response.getErrorMsg(),
                "Module dependency resolution timed out. Please try again or check your network connection.");
    }
}

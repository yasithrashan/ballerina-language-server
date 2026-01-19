/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

import com.google.gson.JsonObject;
import io.ballerina.designmodelgenerator.extension.request.CodeMapRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for getting the code map for a package.
 *
 * @since 1.0.0
 */
public class CodeMapGeneratorTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        CodeMapRequest request = new CodeMapRequest(getSourcePath(testConfig.source()));
        JsonObject codeMapResponse = getResponseAndCloseFile(request, testConfig.source());
        JsonObject files = codeMapResponse.getAsJsonObject("files");

        if (!files.equals(testConfig.output())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.source(), files);
               updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(files, testConfig.output());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String[] skipList() {
        return new String[]{
                // TODO: Need to replace this with the latest ai agent implementation
                "agent.json",
                // TODO: Investigate why the following test fails intermittently in Windows
                "graphql.json",
                // TODO: Include this after discussing how to integrate submodules into the artifacts tree
                "persist.json",
                "function.json", "http_service.json", "kafka.json",
                "listener.json", "np.json", "rabbitmq.json", "service_class.json",
                "tcp.json", "type.json"
        };
    }

    @Override
    protected String getResourceDir() {
        return "codemap";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return CodeMapGeneratorTest.class;
    }

    @Override
    protected String getServiceName() {
        return "designModelService";
    }

    @Override
    protected String getApiName() {
        return "codeMap";
    }


    public record TestConfig(String description, String source, JsonObject output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}

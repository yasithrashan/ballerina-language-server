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

import com.google.gson.JsonObject;
import io.ballerina.designmodelgenerator.extension.request.MarkdownRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for markdown generation from CodeMap (balmd).
 *
 * @since 1.6.0
 */
public class BalmdGeneratorTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        MarkdownRequest request = new MarkdownRequest(getSourcePath(testConfig.source()));
        JsonObject markdownResponse = getResponseAndCloseFile(request, testConfig.source());

        // Get the generated markdown
        String actualMarkdown = markdownResponse.get("markdown").getAsString();

        // Save the generated markdown as .md file in the config directory
        Path markdownOutputPath = configDir.resolve(config.getFileName().toString().replace(".json", ".md"));
        Files.writeString(markdownOutputPath, actualMarkdown);

        if (!actualMarkdown.equals(testConfig.expectedMarkdown())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.source(), actualMarkdown);
//            updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(actualMarkdown, testConfig.expectedMarkdown());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    private void compareJsonElements(String actual, String expected) {
        System.out.println("Expected markdown:");
        System.out.println(expected);
        System.out.println("\nActual markdown:");
        System.out.println(actual);
    }

    @Override
    protected String getResourceDir() {
        return "balmd";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return BalmdGeneratorTest.class;
    }

    @Override
    protected String getServiceName() {
        return "designModelService";
    }

    @Override
    protected String getApiName() {
        return "markdown";
    }

    public record TestConfig(String description, String source, String expectedMarkdown) {

        public String description() {
            return description == null ? "" : description;
        }

        public String expectedMarkdown() {
            return expectedMarkdown == null ? "" : expectedMarkdown;
        }
    }
}
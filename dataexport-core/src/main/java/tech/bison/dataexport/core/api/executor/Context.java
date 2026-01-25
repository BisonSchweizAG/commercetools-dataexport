/*
 * Copyright (C) 2000 - 2026 Bison Schweiz AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.bison.dataexport.core.api.executor;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import tech.bison.dataexport.core.api.configuration.CommercetoolsProperties;
import tech.bison.dataexport.core.api.configuration.Configuration;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;

import java.util.Map;

public class Context {

    private final Configuration configuration;

    public Context(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return The commercetools api root object.
     */
    public ProjectApiRoot getProjectApiRoot() {
        if (configuration.getApiRoot() != null) {
            return configuration.getApiRoot();
        }
        return createProjectApiRoot(configuration.getApiProperties());
    }

    public Map<ExportableResourceType, DataExportProperties> getResourceExportProperties() {
        return configuration.getResourceExportProperties();
    }

    private ProjectApiRoot createProjectApiRoot(CommercetoolsProperties properties) {
        return ApiRootBuilder.of().defaultClient(
                        ClientCredentials.of().withClientId(properties.clientId())
                                .withClientSecret(properties.clientSecret())
                                .build(),
                        properties.authUrl(), properties.apiUrl())
                .build(properties.projectKey());
    }

}

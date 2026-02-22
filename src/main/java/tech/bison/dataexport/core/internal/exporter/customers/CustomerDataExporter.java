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
package tech.bison.dataexport.core.internal.exporter.customers;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.CustomerPagedQueryResponse;
import tech.bison.dataexport.core.api.executor.Context;
import tech.bison.dataexport.core.api.executor.DataExporter;
import tech.bison.dataexport.core.api.executor.DataWriter;

public class CustomerDataExporter implements DataExporter {

    static final Long QUERY_RESULT_LIMIT = 50L;

    @Override
    public void export(Context context, DataWriter dataWriter) {
        var customersResponse = context.getProjectApiRoot().customers().get().withLimit(QUERY_RESULT_LIMIT)
                .withSort("createdAt desc")
                .executeBlocking()
                .getBody();
        customersResponse.getResults().forEach(dataWriter::writeRow);
        for (int i = 1; i < customersResponse.getTotalPages(); i++) {
            customersResponse = loadCustomersPage(context.getProjectApiRoot(), i * QUERY_RESULT_LIMIT);
            customersResponse.getResults().forEach(dataWriter::writeRow);
        }
    }

    private CustomerPagedQueryResponse loadCustomersPage(ProjectApiRoot projectApiRoot, Long offset) {
        return projectApiRoot.customers().get()
                .withLimit(QUERY_RESULT_LIMIT)
                .withOffset(offset)
                .withSort("createdAt desc")
                .executeBlocking()
                .getBody();
    }
}

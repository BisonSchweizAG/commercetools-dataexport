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
    private long queryResultLimit = 50L;

    @Override
    public void export(Context context, String deltaLoadFilter, DataWriter dataWriter) {
        String lastId = null;
        boolean hasMore = true;
        while (hasMore) {
            var customersResponse = loadCustomersPage(context.getProjectApiRoot(), deltaLoadFilter, lastId);
            customersResponse.getResults().forEach(dataWriter::writeRow);
            if (!customersResponse.getResults().isEmpty()) {
                lastId = customersResponse.getResults().getLast().getId();
                hasMore = customersResponse.getResults().size() == queryResultLimit;
            } else {
                hasMore = false;
            }
        }
    }

    private CustomerPagedQueryResponse loadCustomersPage(ProjectApiRoot projectApiRoot, String deltaLoadFilter, String lastId) {
        var request = projectApiRoot.customers().get()
                .withLimit(queryResultLimit)
                .withWithTotal(false)
                .withSort("id asc");
        if (lastId != null) {
            request = request.withWhere(String.format("id > \"%s\"", lastId));
        }
        if (deltaLoadFilter != null) {
            request = request.withWhere(deltaLoadFilter);
        }
        return request.executeBlocking().getBody();
    }

    void setQueryResultLimit(long queryResultLimit) {
        this.queryResultLimit = queryResultLimit;
    }
}

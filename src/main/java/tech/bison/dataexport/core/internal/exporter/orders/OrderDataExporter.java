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
package tech.bison.dataexport.core.internal.exporter.orders;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.order.OrderPagedQueryResponse;
import tech.bison.dataexport.core.api.executor.Context;
import tech.bison.dataexport.core.api.executor.DataExporter;
import tech.bison.dataexport.core.api.executor.DataWriter;

public class OrderDataExporter implements DataExporter {
    static final String LINE_ITEMS_VARIANT_ATTRIBUTES = "lineItems[*].variant.attributes[*].value";
    private Long queryResultLimit = 50L;

    @Override
    public void export(Context context, String deltaLoadFilter, DataWriter dataWriter) {
        String lastId = null;
        boolean hasMore = true;
        while (hasMore) {
            var ordersResponse = loadOrdersPage(context.getProjectApiRoot(), deltaLoadFilter, lastId);
            ordersResponse.getResults().forEach(dataWriter::writeRow);
            if (!ordersResponse.getResults().isEmpty()) {
                lastId = ordersResponse.getResults().getLast().getId();
                hasMore = ordersResponse.getResults().size() == queryResultLimit;
            } else {
                hasMore = false;
            }
        }
    }

    private OrderPagedQueryResponse loadOrdersPage(ProjectApiRoot projectApiRoot, String deltaLoadFilter, String lastId) {
        var request = projectApiRoot.orders().get()
                .withLimit(queryResultLimit)
                .withExpand(LINE_ITEMS_VARIANT_ATTRIBUTES)
                .withWithTotal(false)
                .withSort("id asc");
        if (lastId != null) {
            request = request.addWhere(String.format("id > \"%s\"", lastId));
        }
        if (deltaLoadFilter != null) {
            request = request.addWhere(deltaLoadFilter);
        }
        return request.executeBlocking().getBody();
    }

    void setQueryResultLimit(long queryResultLimit) {
        this.queryResultLimit = queryResultLimit;
    }
}

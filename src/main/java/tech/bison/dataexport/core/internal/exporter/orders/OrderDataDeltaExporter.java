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
import tech.bison.dataexport.core.api.executor.ExportableResourceType;
import tech.bison.dataexport.core.internal.exporter.common.ExportInfoRepository;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class OrderDataDeltaExporter implements DataExporter {

    static final Long QUERY_RESULT_LIMIT = 50L;
    static final String LINE_ITEMS_VARIANT_ATTRIBUTES = "lineItems[*].variant.attributes[*].value";
    private final ExportInfoRepository exportInfoRepository;

    public OrderDataDeltaExporter(ExportInfoRepository exportInfoRepository) {
        this.exportInfoRepository = exportInfoRepository;
    }

    @Override
    public void export(Context context, DataWriter dataWriter) {
        var deltaExportInfo = exportInfoRepository.getCurrent(context);
        var lastExportDate = deltaExportInfo.exportTimestamps().get(getResourceKey());
        String whereClause = getWhereClause(lastExportDate);

        var ordersResponse = loadOrdersPage(context.getProjectApiRoot(), 0L, whereClause);
        ordersResponse.getResults().forEach(dataWriter::writeRow);

        for (int i = 1; i < ordersResponse.getTotalPages(); i++) {
            ordersResponse = loadOrdersPage(context.getProjectApiRoot(), i * QUERY_RESULT_LIMIT, whereClause);
            ordersResponse.getResults().forEach(dataWriter::writeRow);
        }

        var newTimestamps = new HashMap<>(deltaExportInfo.exportTimestamps());
        newTimestamps.put(getResourceKey(), ZonedDateTime.now(context.getClock().withZone(ZoneOffset.UTC)));
        exportInfoRepository.update(context, newTimestamps, deltaExportInfo.documentVersion());
    }

    private static String getResourceKey() {
        return ExportableResourceType.ORDER.getPluralName();
    }

    private static String getWhereClause(ZonedDateTime lastExportDate) {
        if (lastExportDate == null) {
            return null;
        }
        String formatted = DateTimeFormatter.ISO_INSTANT.format(lastExportDate.toInstant());
        return "lastModifiedAt > \"" + formatted + "\"";
    }

    private OrderPagedQueryResponse loadOrdersPage(ProjectApiRoot projectApiRoot, Long offset, String whereClause) {
        var request = projectApiRoot.orders().get()
                .withLimit(QUERY_RESULT_LIMIT)
                .withOffset(offset)
                .withExpand(LINE_ITEMS_VARIANT_ATTRIBUTES)
                .withSort("lastModifiedAt asc");
        if (offset != null) {
            request = request.withOffset(offset);
        }
        if (whereClause != null) {
            request = request.withWhere(whereClause);
        }
        return request.executeBlocking().getBody();
    }
}

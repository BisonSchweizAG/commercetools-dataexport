/*
 * Copyright (C) 2000 - 2026 Bison Schweiz AG
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.bison.dataexport.core.internal.exporter.orders;

import com.commercetools.api.models.order.Order;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.bison.dataexport.core.api.configuration.CommercetoolsProperties;
import tech.bison.dataexport.core.api.configuration.FluentConfiguration;
import tech.bison.dataexport.core.api.executor.Context;
import tech.bison.dataexport.core.api.executor.DataWriter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@WireMockTest
class OrderDataExporterIntegrationTest {

    private Context context;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
        var configuration = new FluentConfiguration().withApiProperties(
                new CommercetoolsProperties("test", "test", baseUrl, baseUrl + "/auth", "integrationtest"));
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse().withBodyFile("token.json")));
        context = new Context(configuration);
    }

    @Test
    void export_fullExportWithMultiplePages_fetchAllOrdersAndWrite() {
        var orderDataExporter = new OrderDataExporter();
        orderDataExporter.setQueryResultLimit(1L); // override page limit to keep payload files simple
        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("expand", equalTo(OrderDataExporter.LINE_ITEMS_VARIANT_ATTRIBUTES))
                .withQueryParam("sort", equalTo("id asc"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("orders-page1.json")));

        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("where", equalTo("id > \"92f5a867-bf19-47ab-982c-6720a03a3921\""))
                .withQueryParam("sort", equalTo("id asc"))
                .withQueryParam("expand", equalTo(OrderDataExporter.LINE_ITEMS_VARIANT_ATTRIBUTES))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("orders-page2.json")));

        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("where", equalTo("id > \"ef4b1425-3c39-4380-bff1-7d683b1e237f\""))
                .withQueryParam("sort", equalTo("id asc"))
                .withQueryParam("expand", equalTo(OrderDataExporter.LINE_ITEMS_VARIANT_ATTRIBUTES))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("empty-results.json")));

        var orderDataWriter = mock(DataWriter.class);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        doNothing().when(orderDataWriter).writeRow(orderCaptor.capture());

        orderDataExporter.export(context, null, orderDataWriter);

        var allCapturedOrders = orderCaptor.getAllValues();
        assertThat(allCapturedOrders).hasSize(2);
        assertThat(allCapturedOrders.get(0).getId()).isEqualTo("92f5a867-bf19-47ab-982c-6720a03a3921");
        assertThat(allCapturedOrders.get(1).getId()).isEqualTo("ef4b1425-3c39-4380-bff1-7d683b1e237f");
    }
}

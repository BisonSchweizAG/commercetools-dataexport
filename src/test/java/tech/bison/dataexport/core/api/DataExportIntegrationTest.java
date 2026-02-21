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
package tech.bison.dataexport.core.api;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tech.bison.dataexport.core.api.configuration.CommercetoolsProperties;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@WireMockTest(httpPort = 8087)
class DataExportIntegrationTest {

    @Test
    void execute_ordersExport_uploadExpectedCsvPayload() throws IOException {
        stubFor(post(urlEqualTo("/auth")).willReturn(aResponse().withBodyFile("token.json")));
        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("expand", equalTo("lineItems[*].variant.attributes[*]"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/json")
                                .withBodyFile("orders-execute-single-page.json")));

        CloudStorageUploader cloudStorageUploader = Mockito.mock(CloudStorageUploader.class);
        var dataExport = DataExport.configure()
                .withApiProperties(
                        new CommercetoolsProperties("test", "test", "http://localhost:8087", "http://localhost:8087/auth",
                                "integrationtest"))
                .withExportFields(ExportableResourceType.ORDER, List.of(
                        "orderNumber",
                        "customerId",
                        "lineItems.id", "lineItems.quantity", "lineItems.variant.attributes.color", "lineItems.variant.attributes.supplierCategory.obj.key"))
                .withClock(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")))
                .withCloudStorageUploader(cloudStorageUploader)
                .load();

        var result = dataExport.execute();

        assertThat(result.getResourceSummary(ExportableResourceType.ORDER)).isEqualTo(ResourceExportResult.SUCCESS);

        ArgumentCaptor<byte[]> uploadedPayloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(cloudStorageUploader)
                .upload(Mockito.eq("orders/orders_2026_01_01_10_00_00.csv"), uploadedPayloadCaptor.capture());

        String actualPayload = new String(uploadedPayloadCaptor.getValue(), StandardCharsets.UTF_8);
        String expectedPayload = readResource("expected-payloads/data-export-orders.csv");
        assertThat(actualPayload).isEqualTo(expectedPayload);

        WireMock.verify(getRequestedFor(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("expand", equalTo("lineItems[*].variant.attributes[*]")));
    }

    private String readResource(String resourcePath) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(stream).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

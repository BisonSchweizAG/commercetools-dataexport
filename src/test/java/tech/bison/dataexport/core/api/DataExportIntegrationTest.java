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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tech.bison.dataexport.core.api.configuration.CommercetoolsProperties;
import tech.bison.dataexport.core.api.configuration.ExportMode;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@WireMockTest
class DataExportIntegrationTest {

    @Test
    void execute_ordersFullExport_uploadExpectedCsvPayload(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        stubFor(post(urlEqualTo("/auth")).willReturn(aResponse().withBodyFile("token.json")));
        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("expand", equalTo("lineItems[*].variant.attributes[*].value"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/json")
                                .withBodyFile("orders-execute-single-page.json")));

        ExportDataUploader cloudStorageUploader = Mockito.mock(ExportDataUploader.class);
        String baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
        var dataExport = DataExport.configure()
                .withApiProperties(
                        new CommercetoolsProperties("test", "test", baseUrl, baseUrl + "/auth",
                                "integrationtest"))
                .withOrderExport(List.of(
                        "orderNumber",
                        "customerId",
                        "lineItems.id", "lineItems.quantity", "lineItems.variant.attributes.color",
                        "lineItems.variant.attributes.supplierCategory.obj.key"), ExportMode.FULL)
                .withClock(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")))
                .withUploader(cloudStorageUploader)
                .load();

        var result = dataExport.execute();

        assertThat(result.getResourceSummary("orders")).isEqualTo(ResourceExportResult.SUCCESS);

        ArgumentCaptor<byte[]> uploadedPayloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(cloudStorageUploader)
                .upload(Mockito.eq("orders/orders_2026_01_01_10_00_00.csv"), uploadedPayloadCaptor.capture());

        String actualPayload = new String(uploadedPayloadCaptor.getValue(), StandardCharsets.UTF_8);
        String expectedPayload = readResource("expected-payloads/data-export-orders.csv");
        assertThat(normalizePayload(actualPayload)).isEqualTo(normalizePayload(expectedPayload));
    }

    @Test
    void execute_ordersDeltaExport_uploadExpectedCsvPayload(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        stubFor(post(urlEqualTo("/auth")).willReturn(aResponse().withBodyFile("token.json")));
        stubFor(get(urlPathEqualTo("/integrationtest/custom-objects/dataExport/dataExportKey"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("export-info-custom-object.json")));
        stubFor(post(urlPathEqualTo("/integrationtest/custom-objects"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBodyFile("export-info-custom-object-updated.json")));
        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("where", equalTo("lastModifiedAt > \"2026-01-01T10:00:00Z\""))
                .withQueryParam("expand", equalTo("lineItems[*].variant.attributes[*].value"))
                .willReturn(
                        aResponse().withHeader("Content-Type", "application/json")
                                .withBodyFile("orders-execute-single-page.json")));

        ExportDataUploader cloudStorageUploader = Mockito.mock(ExportDataUploader.class);
        String baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
        var dataExport = DataExport.configure()
                .withApiProperties(
                        new CommercetoolsProperties("test", "test", baseUrl, baseUrl + "/auth",
                                "integrationtest"))
                .withOrderExport(List.of(
                        "orderNumber",
                        "customerId",
                        "lineItems.id", "lineItems.quantity", "lineItems.variant.attributes.color",
                        "lineItems.variant.attributes.supplierCategory.obj.key"), ExportMode.DELTA)
                .withClock(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")))
                .withUploader(cloudStorageUploader)
                .load();

        var result = dataExport.execute();

        assertThat(result.getResourceSummary("orders")).isEqualTo(ResourceExportResult.SUCCESS);

        ArgumentCaptor<byte[]> uploadedPayloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(cloudStorageUploader)
                .upload(Mockito.eq("orders/orders_2026_01_01_10_00_00.csv"), uploadedPayloadCaptor.capture());

        String actualPayload = new String(uploadedPayloadCaptor.getValue(), StandardCharsets.UTF_8);
        String expectedPayload = readResource("expected-payloads/data-export-orders.csv");
        assertThat(normalizePayload(actualPayload)).isEqualTo(normalizePayload(expectedPayload));
    }

    @Test
    void execute_customersExport_uploadExpectedCsvPayload(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        stubFor(post(urlEqualTo("/auth")).willReturn(aResponse().withBodyFile("token.json")));
        stubFor(get(urlPathEqualTo("/integrationtest/customers"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBodyFile("customers-single-page.json")));

        ExportDataUploader cloudStorageUploader = Mockito.mock(ExportDataUploader.class);
        String baseUrl = wireMockRuntimeInfo.getHttpBaseUrl();
        var dataExport = DataExport.configure()
                .withApiProperties(
                        new CommercetoolsProperties("test", "test", baseUrl, baseUrl + "/auth",
                                "integrationtest"))
                .withCustomerExport(List.of("id", "email", "customerNumber"))
                .withClock(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")))
                .withUploader(cloudStorageUploader)
                .load();

        var result = dataExport.execute();

        assertThat(result.getResourceSummary("customers")).isEqualTo(ResourceExportResult.SUCCESS);

        ArgumentCaptor<byte[]> uploadedPayloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(cloudStorageUploader)
                .upload(Mockito.eq("customers/customers_2026_01_01_10_00_00.csv"), uploadedPayloadCaptor.capture());

        String actualPayload = new String(uploadedPayloadCaptor.getValue(), StandardCharsets.UTF_8);
        String expectedPayload = readResource("expected-payloads/data-export-customers.csv");
        assertThat(normalizePayload(actualPayload)).isEqualTo(normalizePayload(expectedPayload));

        verify(getRequestedFor(urlPathEqualTo("/integrationtest/customers")));
    }

    private String readResource(String resourcePath) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(stream).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String normalizePayload(String payload) {
        String normalized = payload.replace("\r\n", "\n").replace("\r", "\n");
        return normalized.replaceAll("\\s+$", "");
    }
}

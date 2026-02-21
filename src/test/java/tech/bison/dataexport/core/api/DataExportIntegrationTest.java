package tech.bison.dataexport.core.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import tech.bison.dataexport.core.api.configuration.CommercetoolsProperties;
import tech.bison.dataexport.core.api.configuration.GcpCloudStorageProperties;
import tech.bison.dataexport.core.api.executor.ExportableResourceType;
import tech.bison.dataexport.core.internal.storage.gcp.GcpCloudStorageUploader;

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

    try (MockedConstruction<GcpCloudStorageUploader> mockedUploaderConstruction = Mockito.mockConstruction(
        GcpCloudStorageUploader.class)) {
      var dataExport = DataExport.configure()
          .withApiProperties(
              new CommercetoolsProperties("test", "test", "http://localhost:8087", "http://localhost:8087/auth",
                  "integrationtest"))
          .withExportFields(ExportableResourceType.ORDER, List.of(
              "orderNumber",
              "customerId",
              "lineItems.id",
              "lineItems.quantity"))
          .withClock(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")))
          .withGcpCloudStorageProperties(new GcpCloudStorageProperties("project-id", "bucket-name", null))
          .load();

      var result = dataExport.execute();

      assertThat(result.getResourceSummary(ExportableResourceType.ORDER)).isEqualTo(ResourceExportResult.SUCCESS);
      assertThat(mockedUploaderConstruction.constructed()).hasSize(1);

      var uploader = mockedUploaderConstruction.constructed().get(0);
      ArgumentCaptor<byte[]> uploadedPayloadCaptor = ArgumentCaptor.forClass(byte[].class);
      verify(uploader).upload(Mockito.eq("orders/orders_2026_01_01_10_00_00.csv"), uploadedPayloadCaptor.capture());

      String actualPayload = new String(uploadedPayloadCaptor.getValue(), StandardCharsets.UTF_8);
      String expectedPayload = readResource("expected-payloads/data-export-orders.csv");
      assertThat(actualPayload).isEqualTo(expectedPayload);
    }

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

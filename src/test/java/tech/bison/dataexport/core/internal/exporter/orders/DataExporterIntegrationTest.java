package tech.bison.dataexport.core.internal.exporter.orders;

import com.commercetools.api.models.order.Order;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@WireMockTest(httpPort = 8087)
class DataExporterIntegrationTest {
    private Context context;

    @BeforeEach
    void setUp() {
        var configuration = new FluentConfiguration().withApiProperties(new CommercetoolsProperties("test", "test", "http://localhost:8087", "http://localhost:8087/auth", "integrationtest"));
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse().withBodyFile("token.json")));
        context = new Context(configuration);
    }

    @Test
    void export_allOrdersWithinPageLimit_fetchAllOrdersAndWrite() {
        var orderDataExporter = new OrderDataExporter();

        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("orders-single-page.json")));

        var orderDataWriter = mock(DataWriter.class);

        orderDataExporter.export(context, orderDataWriter);

        verify(orderDataWriter).writeRow(any(Order.class));
    }

    @Test
    void export_allOrdersMultiplePages_fetchAllOrdersAndWrite() {
        var orderDataExporter = new OrderDataExporter();

        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("orders-page1.json")));

        stubFor(get(urlPathEqualTo("/integrationtest/orders"))
                .withQueryParam("offset", equalTo("50"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("orders-page2.json")));

        var orderDataWriter = mock(DataWriter.class);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        doNothing().when(orderDataWriter).writeRow(orderCaptor.capture());

        orderDataExporter.export(context, orderDataWriter);

        var allCapturedOrders = orderCaptor.getAllValues();
        assertThat(allCapturedOrders).hasSize(2);
        assertThat(allCapturedOrders.get(0).getId()).isEqualTo("92f5a867-bf19-47ab-982c-6720a03a3921");
        assertThat(allCapturedOrders.get(1).getId()).isEqualTo("ef4b1425-3c39-4380-bff1-7d683b1e237f");
    }
}
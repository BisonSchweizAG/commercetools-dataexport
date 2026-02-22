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
package tech.bison.dataexport.core.internal.exporter.customers;

import com.commercetools.api.models.customer.Customer;
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
class CustomerDataExporterIntegrationTest {
    private Context context;

    @BeforeEach
    void setUp() {
        var configuration = new FluentConfiguration().withApiProperties(new CommercetoolsProperties("test", "test", "http://localhost:8087", "http://localhost:8087/auth", "integrationtest"));
        stubFor(post(urlEqualTo("/auth"))
                .willReturn(aResponse().withBodyFile("token.json")));
        context = new Context(configuration);
    }

    @Test
    void export_allCustomersWithinPageLimit_fetchAllCustomersAndWrite() {
        var customerDataExporter = new CustomerDataExporter();

        stubFor(get(urlPathEqualTo("/integrationtest/customers"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("customers-single-page.json")));

        var customerDataWriter = mock(DataWriter.class);

        customerDataExporter.export(context, customerDataWriter);

        verify(customerDataWriter).writeRow(any(Customer.class));
    }

    @Test
    void export_allCustomersMultiplePages_fetchAllCustomersAndWrite() {
        var customerDataExporter = new CustomerDataExporter();

        stubFor(get(urlPathEqualTo("/integrationtest/customers"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("customers-page1.json")));

        stubFor(get(urlPathEqualTo("/integrationtest/customers"))
                .withQueryParam("offset", equalTo("50"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("customers-page2.json")));

        var customerDataWriter = mock(DataWriter.class);
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        doNothing().when(customerDataWriter).writeRow(customerCaptor.capture());

        customerDataExporter.export(context, customerDataWriter);

        var allCapturedCustomers = customerCaptor.getAllValues();
        assertThat(allCapturedCustomers).hasSize(2);
        assertThat(allCapturedCustomers.get(0).getId()).isEqualTo("f8ef5f9f-1760-4f1a-85e4-fc90e750efe2");
        assertThat(allCapturedCustomers.get(1).getId()).isEqualTo("d76b9bca-8a13-46f7-a0a7-a4e7fb1d3272");
    }
}

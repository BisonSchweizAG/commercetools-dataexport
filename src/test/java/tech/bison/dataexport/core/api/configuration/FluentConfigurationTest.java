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
package tech.bison.dataexport.core.api.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static tech.bison.dataexport.core.api.executor.ExportableResourceType.CUSTOMER;
import static tech.bison.dataexport.core.api.executor.ExportableResourceType.ORDER;

import com.commercetools.api.client.ProjectApiRoot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tech.bison.dataexport.core.api.exception.DataExportException;

class FluentConfigurationTest {

  @Test
  void load_withMissingApiConfiguration_throwsException() {
    var configuration = new FluentConfiguration()
        .withGcpCloudStorageProperties(createValidCloudStorageConfiguration())
        .withExportFields(Map.of(ORDER, new DataExportProperties(ORDER, List.of("id"))));

    assertThatThrownBy(configuration::load)
        .isInstanceOf(DataExportException.class)
        .hasMessage("Missing commercetools api configuration. Either use withApiProperties() or withApiRoot().");
  }

  @Test
  void load_withEmptyExportFieldsMap_throwsException() {
    var configuration = new FluentConfiguration()
        .withApiRoot(mock(ProjectApiRoot.class))
        .withGcpCloudStorageProperties(createValidCloudStorageConfiguration());

    assertThatThrownBy(configuration::load)
        .isInstanceOf(DataExportException.class)
        .hasMessage("At least one export type must be configured.");
  }

  @Test
  void load_withExportTypeWithoutFields_throwsException() {
    var configuration = new FluentConfiguration()
        .withApiRoot(mock(ProjectApiRoot.class))
        .withGcpCloudStorageProperties(createValidCloudStorageConfiguration())
        .withExportFields(Map.of(ORDER, new DataExportProperties(ORDER, List.of())));

    assertThatThrownBy(configuration::load)
        .isInstanceOf(DataExportException.class)
        .hasMessage("At least one export type has no fields configured.");
  }

  @Test
  void load_withMissingGcpCloudStorageProperties_throwsException() {
    var configuration = new FluentConfiguration()
        .withApiRoot(mock(ProjectApiRoot.class))
        .withExportFields(Map.of(ORDER, new DataExportProperties(ORDER, List.of("id"))));

    assertThatThrownBy(configuration::load)
        .isInstanceOf(DataExportException.class)
        .hasMessage("GCP cloud storage configuration is missing.");
  }

  @Test
  void load_withApiProperties_returnsDataExport() {
    var configuration = new FluentConfiguration()
        .withApiProperties(createValidCommercetoolsProperties())
        .withGcpCloudStorageProperties(createValidCloudStorageConfiguration())
        .withExportFields(Map.of(ORDER, new DataExportProperties(ORDER, List.of("id"))));

    assertThat(configuration.load()).isNotNull();
  }

  @Test
  void load_withAllRequiredConfigurations_returnsDataExport() {
    var configuration = new FluentConfiguration()
        .withApiRoot(mock(ProjectApiRoot.class))
        .withGcpCloudStorageProperties(createValidCloudStorageConfiguration())
        .withExportFields(Map.of(ORDER, new DataExportProperties(ORDER, List.of("id"))));

    assertThat(configuration.load()).isNotNull();
  }

  @Test
  void load_withMultipleExportTypes_returnsDataExport() {
    var configuration = new FluentConfiguration()
        .withApiRoot(mock(ProjectApiRoot.class))
        .withGcpCloudStorageProperties(createValidCloudStorageConfiguration())
        .withExportFields(Map.of(
            ORDER, new DataExportProperties(ORDER, List.of("id", "orderNumber")),
            CUSTOMER, new DataExportProperties(CUSTOMER, List.of("id", "name"))
        ));

    assertThat(configuration.load()).isNotNull();
  }

  private GcpCloudStorageProperties createValidCloudStorageConfiguration() {
    return new GcpCloudStorageProperties("projectId", "bucketName", null);
  }

  private CommercetoolsProperties createValidCommercetoolsProperties() {
    return new CommercetoolsProperties("clientId", "clientSecret", "authUrl", "apiUrl", "projectKey");
  }
}
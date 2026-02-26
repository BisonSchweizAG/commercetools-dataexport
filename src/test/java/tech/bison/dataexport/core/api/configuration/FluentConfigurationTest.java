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

import com.commercetools.api.client.ProjectApiRoot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bison.dataexport.core.api.exception.DataExportException;
import tech.bison.dataexport.core.api.executor.DataExporter;
import tech.bison.dataexport.core.api.executor.DataWriterProvider;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static tech.bison.dataexport.core.api.executor.ExportableResourceType.CUSTOMER;
import static tech.bison.dataexport.core.api.executor.ExportableResourceType.ORDER;

@ExtendWith(MockitoExtension.class)
class FluentConfigurationTest {

    @Mock
    private ExportDataUploader exportDataUploader;
    @Mock
    private DataExporter customDataExporter;
    @Mock
    private DataWriterProvider customDataWriterProvider;

    @Test
    void load_withMissingApiConfiguration_throwsException() {
        var configuration = new FluentConfiguration()
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of("id"));

        assertThatThrownBy(configuration::load)
                .isInstanceOf(DataExportException.class)
                .hasMessage("Missing commercetools api configuration. Either use withApiProperties() or withApiRoot().");
    }

    @Test
    void load_withEmptyExportFieldsMap_throwsException() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader);

        assertThatThrownBy(configuration::load)
                .isInstanceOf(DataExportException.class)
                .hasMessage("At least one export type must be configured.");
    }

    @Test
    void load_withExportTypeWithoutFields_throwsException() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of());

        assertThatThrownBy(configuration::load)
                .isInstanceOf(DataExportException.class)
                .hasMessage("At least one export type has no fields configured.");
    }

    @Test
    void load_withMissingDataExportUploader_throwsException() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withExportFields(ORDER, List.of("id"));

        assertThatThrownBy(configuration::load)
                .isInstanceOf(DataExportException.class)
                .hasMessage("No data export uploader configured.");
    }

    @Test
    void load_withApiProperties_returnsDataExport() {
        var configuration = new FluentConfiguration()
                .withApiProperties(createValidCommercetoolsProperties())
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of("id"));

        assertThat(configuration.load()).isNotNull();
    }

    @Test
    void load_withAllRequiredConfigurations_returnsDataExport() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of("id"));

        assertThat(configuration.getExportDataUploaders()).containsExactly(exportDataUploader);
        assertThat(configuration.load()).isNotNull();
    }

    @Test
    void load_withMultipleExportTypes_returnsDataExport() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of("id", "orderNumber"))
                .withExportFields(CUSTOMER, List.of("id", "name"));

        assertThat(configuration.load()).isNotNull();
    }

    @Test
    void withGcpCloudStorageProperties_addsGcpUploader() {
        var gcpProperties = new GcpCloudStorageProperties("projectId", "bucketName", "credentialPath");
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withGcpCloudStorageProperties(gcpProperties)
                .withExportFields(ORDER, List.of("id"));

        assertThat(configuration.getExportDataUploaders()).hasSize(1);
        assertThat(configuration.load()).isNotNull();
    }

    @Test
    void load_withInvalidMaxRecordsPerUpload_throwsException() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of("id"))
                .withMaxRecordsPerUpload(0);

        assertThatThrownBy(configuration::load)
                .isInstanceOf(DataExportException.class)
                .hasMessage("maxRecordsPerUpload must be greater than 0.");
    }

    @Test
    void load_withMaxRecordsPerUpload_setsValue() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader)
                .withExportFields(ORDER, List.of("id"))
                .withMaxRecordsPerUpload(500);

        assertThat(configuration.getMaxRecordsPerUpload()).isEqualTo(500);
        assertThat(configuration.load()).isNotNull();
    }

    @Test
    void load_withCustomExporter_registersCustomExecution() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader)
                .withCustomExporter("custom-export", customDataExporter, customDataWriterProvider);

        var dataExportExecution = configuration.getDataExportExecutions().get("custom-export");
        assertThat(dataExportExecution).isNotNull();
        assertThat(dataExportExecution.dataExporter()).isEqualTo(customDataExporter);
        assertThat(dataExportExecution.dataWriterProvider()).isEqualTo(customDataWriterProvider);
        assertThat(configuration.load()).isNotNull();
    }

    @Test
    void load_withCustomExporterAndBlankExportKey_throwsException() {
        var configuration = new FluentConfiguration()
                .withApiRoot(mock(ProjectApiRoot.class))
                .withUploader(exportDataUploader);

        assertThatThrownBy(() -> configuration.withCustomExporter(" ", customDataExporter,
                customDataWriterProvider))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exportKey must not be blank.");
    }

    private CommercetoolsProperties createValidCommercetoolsProperties() {
        return new CommercetoolsProperties("clientId", "clientSecret", "authUrl", "apiUrl", "projectKey");
    }
}

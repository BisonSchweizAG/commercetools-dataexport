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

package tech.bison.dataexport.core.api.executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;
import tech.bison.dataexport.core.internal.exector.DataExportExecutor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;
import static tech.bison.dataexport.core.api.executor.ExportableResourceType.CUSTOMER;
import static tech.bison.dataexport.core.api.executor.ExportableResourceType.ORDER;

@ExtendWith(MockitoExtension.class)
class DataExportExecutorTest {

    @Mock
    private CloudStorageUploader cloudStorageUploader;
    @Mock
    private DataWriter customerDataWriter;
    @Mock
    private DataWriter orderDataWriter;

    @Test
    public void execute_allDataExportCommands() {
        var context = mock(Context.class);
        var orderProperties = new DataExportProperties(ORDER, List.of());
        var customerProperties = new DataExportProperties(CUSTOMER, List.of());
        when(context.getResourceExportProperties()).thenReturn(Map.of(ORDER, orderProperties, CUSTOMER, customerProperties));
        var exporterSuccess = mock(DataExporter.class);

        var exporterFailure = mock(DataExporter.class);
        doThrow(RuntimeException.class).when(exporterFailure).export(any(), any());

        var executor = createDataExportExecutor(exporterSuccess, exporterFailure);

        DataExportResult result = executor.execute(context);

        assertThat(result.getResourceSummary(ORDER)).isEqualTo(SUCCESS);
        verify(cloudStorageUploader, Mockito.times(1)).upload(any(byte[].class));
        assertThat(result.getResourceSummary(CUSTOMER)).isEqualTo(FAILED);
    }

    private DataExportExecutor createDataExportExecutor(DataExporter exporterSuccess, DataExporter exporterFailure) {
        DataExporterProvider dataExporterProvider = (properties) -> {
            if (properties.resourceType() == ORDER) {
                return exporterSuccess;
            } else {
                return exporterFailure;
            }
        };

        DataWriterProvider dataWriterProvider = (properties, csvPrinter) -> {
            if (properties.resourceType() == ORDER) {
                return customerDataWriter;
            } else {
                return orderDataWriter;
            }
        };

        return new DataExportExecutor(cloudStorageUploader, dataExporterProvider, dataWriterProvider);
    }
}
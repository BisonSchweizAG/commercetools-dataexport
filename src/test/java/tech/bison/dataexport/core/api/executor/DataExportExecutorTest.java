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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;

import com.commercetools.api.models.common.BaseResource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;
import tech.bison.dataexport.core.internal.exector.DataExportExecutor;

@ExtendWith(MockitoExtension.class)
class DataExportExecutorTest {

  @Mock
  private ExportDataUploader exportDataUploader;
  @Mock
  private DataWriter dataWriter;

  @Test
  void execute_allDataExportCommands() {
    var context = mock(Context.class);
    when(context.getClock()).thenReturn(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")));
    when(context.getMaxRecordsPerUpload()).thenReturn(null);
    when(context.getOutputFileExtension()).thenReturn("csv");
    var exporterSuccess = mock(DataExporter.class);

    var exporterFailure = mock(DataExporter.class);
    doThrow(RuntimeException.class).when(exporterFailure).export(any(), any());

    var executor = createDataExportExecutor(exporterSuccess, exporterFailure);
    DataExportResult result = executor.execute(context);

    assertThat(result.getResourceSummary("order")).isEqualTo(SUCCESS);
    verify(exportDataUploader, times(1)).upload(eq("order/order_2026_01_01_10_00_00.csv"),
        any(byte[].class));
    assertThat(result.getResourceSummary("customer")).isEqualTo(FAILED);

  }

  @Test
  void execute_withMaxRecordsPerUploadAndLimitNotReached_uploadSingleFile() {
    var context = mock(Context.class);
    when(context.getClock()).thenReturn(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")));
    when(context.getMaxRecordsPerUpload()).thenReturn(2);
    when(context.getOutputFileExtension()).thenReturn("csv");
    var dataExporter = mock(DataExporter.class);

    Mockito.doAnswer(invocation -> {
      DataWriter writer = invocation.getArgument(1);
      writer.writeRow(mock(BaseResource.class));
      return null;
    }).when(dataExporter).export(any(), any());

    var executor = createDataExportExecutor(dataExporter);
    var result = executor.execute(context);

    assertThat(result.getResourceSummary("order")).isEqualTo(SUCCESS);
    verify(exportDataUploader, times(1)).upload(eq("order/order_2026_01_01_10_00_00.csv"),
        any(byte[].class));
  }

  @Test
  void execute_withMaxRecordsPerUploadAndLimitReached_uploadsChunkedFiles() {
    var context = mock(Context.class);
    when(context.getClock()).thenReturn(Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneId.of("UTC")));
    when(context.getMaxRecordsPerUpload()).thenReturn(2);
    when(context.getOutputFileExtension()).thenReturn("csv");

    var exporter = mock(DataExporter.class);
    Mockito.doAnswer(invocation -> {
      DataWriter writer = invocation.getArgument(1);
      writer.writeRow(mock(BaseResource.class));
      writer.writeRow(mock(BaseResource.class));
      writer.writeRow(mock(BaseResource.class));
      writer.writeRow(mock(BaseResource.class));
      writer.writeRow(mock(BaseResource.class));
      return null;
    }).when(exporter).export(any(), any());

    var executor = createDataExportExecutor(exporter);
    var result = executor.execute(context);

    assertThat(result.getResourceSummary("order")).isEqualTo(SUCCESS);
    verify(exportDataUploader, times(1)).upload(eq("order/order_2026_01_01_10_00_00_part_001.csv"),
        any(byte[].class));
    verify(exportDataUploader, times(1)).upload(eq("order/order_2026_01_01_10_00_00_part_002.csv"),
        any(byte[].class));
    verify(exportDataUploader, times(1)).upload(eq("order/order_2026_01_01_10_00_00_part_003.csv"),
        any(byte[].class));
  }

  private DataExportExecutor createDataExportExecutor(DataExporter exporterSuccess, DataExporter exporterFailure) {
    var orderProperties = new DataExportProperties(List.of());
    var customerProperties = new DataExportProperties(List.of());
    return new DataExportExecutor(List.of(exportDataUploader),
        Map.of("order", new DataExportExecution(orderProperties, exporterSuccess, (_, _) -> dataWriter),
            "customer", new DataExportExecution(customerProperties, exporterFailure, (_, _) -> dataWriter)));
  }

  private DataExportExecutor createDataExportExecutor(DataExporter dataExporter) {
    var orderProperties = new DataExportProperties(List.of());
    return new DataExportExecutor(List.of(exportDataUploader),
        Map.of("order", new DataExportExecution(orderProperties, dataExporter, (_, _) -> dataWriter)));
  }
}

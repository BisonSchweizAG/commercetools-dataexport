/*
 * Copyright (C) 2000 - 2026 Bison Schweiz AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.bison.dataexport.core.internal.exector;

import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;

import com.commercetools.api.models.common.BaseResource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.bison.dataexport.core.api.executor.Context;
import tech.bison.dataexport.core.api.executor.DataExportExecution;
import tech.bison.dataexport.core.api.executor.DataExportResult;
import tech.bison.dataexport.core.api.executor.DataWriter;
import tech.bison.dataexport.core.api.executor.DataWriterProvider;
import tech.bison.dataexport.core.api.upload.ExportDataUploader;

public class DataExportExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(DataExportExecutor.class);
  private final List<ExportDataUploader> exportDataUploaderList;
  private final Map<String, DataExportExecution> dataExportExecutions;

  public DataExportExecutor(List<ExportDataUploader> exportDataUploaderList,
      Map<String, DataExportExecution> dataExportExecutions) {
    this.exportDataUploaderList = exportDataUploaderList;
    this.dataExportExecutions = dataExportExecutions;
  }

  public DataExportResult execute(Context context) {
    DataExportResult dataExportResult = DataExportResult.empty();
    for (var entry : dataExportExecutions.entrySet()) {
      var exportKey = entry.getKey();
      LOG.info("Running data export for resource '{}'.", exportKey);
      try {
        DataExportExecution dataExportExecution = entry.getValue();
        DataWriter dataWriter = new ChunkedUploadDataWriterWrapper(exportKey,
            dataExportExecution.dataExportProperties().fields(), context,
            dataExportExecution.dataWriterProvider());
        dataExportExecution.dataExporter().export(context, dataWriter);
        dataWriter.flush();
        dataExportResult.addResult(exportKey, SUCCESS);
        LOG.info("Data export finished successfully for resource '{}'.", exportKey);
      } catch (Exception ex) {
        dataExportResult.addResult(exportKey, FAILED);
        LOG.error("Error while executing data export for resource '{}'. Continue with next resource type.",
            exportKey, ex);
      }
    }
    return dataExportResult;
  }

  private class ChunkedUploadDataWriterWrapper implements DataWriter {

    private final String exportKey;
    private final Context context;
    private final List<String> fields;
    private final DataWriterProvider dataWriterProvider;
    private final Integer maxRecordsPerUpload;
    private final String outputFileExtension;
    private final List<byte[]> completedChunks = new ArrayList<>();
    private ByteArrayOutputStream outputStream;
    private DataWriter dataWriter;
    private int recordCountInChunk;
    private boolean flushed;

    private ChunkedUploadDataWriterWrapper(String exportKey,
        List<String> fields, Context context,
        DataWriterProvider dataWriterProvider) {
      this.exportKey = exportKey;
      this.fields = fields;
      this.context = context;
      this.dataWriterProvider = dataWriterProvider;
      this.maxRecordsPerUpload = context.getMaxRecordsPerUpload();
      this.outputFileExtension = context.getOutputFileExtension();
      rotateChunk();
    }

    @Override
    public void writeRow(BaseResource object) {
      if (flushed) {
        throw new IllegalStateException("Cannot write to flushed data writer.");
      }
      if (maxRecordsPerUpload != null && recordCountInChunk >= maxRecordsPerUpload) {
        flushChunkAndRotate();
      }
      dataWriter.writeRow(object);
      recordCountInChunk++;
    }

    @Override
    public void flush() {
      if (flushed) {
        return;
      }
      storeChunk();
      uploadChunks();
      flushed = true;
    }

    private void flushChunkAndRotate() {
      storeChunk();
      rotateChunk();
    }

    private void rotateChunk() {
      outputStream = new ByteArrayOutputStream();
      dataWriter = dataWriterProvider.create(fields, outputStream);
      recordCountInChunk = 0;
    }

    private void storeChunk() {
      dataWriter.flush();
      completedChunks.add(outputStream.toByteArray());
      try {
        outputStream.close();
      } catch (IOException ex) {
        LOG.debug("Failed to close output stream for resource '{}'.", exportKey, ex);
      }
    }

    private void uploadChunks() {
      boolean useChunkSuffix = maxRecordsPerUpload != null && completedChunks.size() > 1;
      for (int i = 0; i < completedChunks.size(); i++) {
        Integer currentChunk = useChunkSuffix ? i + 1 : null;
        byte[] bytes = completedChunks.get(i);
        exportDataUploaderList.forEach(uploader -> uploader.upload(
            getBlobName(exportKey, context.getClock(), outputFileExtension, currentChunk), bytes));
      }
    }

    private String getBlobName(String exportKey, Clock clock, String fileExtension,
        Integer chunkNumber) {
      String normalizedExtension = fileExtension.startsWith(".") ? fileExtension.substring(1) : fileExtension;
      String baseName = String.format("%s/%s_%s", exportKey, exportKey,
          LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")));
      if (chunkNumber == null) {
        return String.format("%s.%s", baseName, normalizedExtension);
      }
      return String.format("%s_part_%03d.%s", baseName, chunkNumber, normalizedExtension);
    }
  }
}

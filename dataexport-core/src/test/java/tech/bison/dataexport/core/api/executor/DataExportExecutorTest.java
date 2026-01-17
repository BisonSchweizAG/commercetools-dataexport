/*
 * Copyright (C) 2000 - 2024 Bison Schweiz AG
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
import tech.bison.dataexport.core.api.command.DataExportResult;
import tech.bison.dataexport.core.api.command.DataLoader;
import tech.bison.dataexport.core.api.command.ExportableResourceType;
import tech.bison.dataexport.core.api.command.ResourceExportData;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static tech.bison.dataexport.core.api.ResourceExportResult.FAILED;
import static tech.bison.dataexport.core.api.ResourceExportResult.SUCCESS;
import static tech.bison.dataexport.core.api.command.ExportableResourceType.CUSTOMER;
import static tech.bison.dataexport.core.api.command.ExportableResourceType.ORDER;

@ExtendWith(MockitoExtension.class)
class DataExportExecutorTest {

    @Mock
    private CloudStorageUploader cloudStorageUploader;

    @Test
    public void execute_allDataExportCommands() {
        var context = mock(Context.class);
        var successfulExportLoader = mock(DataLoader.class);
        var orderExportData = new ResourceExportData(ExportableResourceType.ORDER, new byte[0]);

        when(successfulExportLoader.load(context)).thenReturn(orderExportData);
        when(successfulExportLoader.getResourceType()).thenReturn(ORDER);

        var failingExportLoader = mock(DataLoader.class);
        when(failingExportLoader.load(context)).thenThrow(RuntimeException.class);
        when(failingExportLoader.getResourceType()).thenReturn(CUSTOMER);

        var executor = new DataExportExecutor(cloudStorageUploader);
        List<DataLoader> commands = List.of(successfulExportLoader, failingExportLoader);

        DataExportResult result = executor.execute(context, commands);

        assertThat(result.getResourceSummary(ORDER)).isEqualTo(SUCCESS);
        verify(cloudStorageUploader, Mockito.times(1)).upload(any(ResourceExportData.class));
        assertThat(result.getResourceSummary(CUSTOMER)).isEqualTo(FAILED);
    }
}
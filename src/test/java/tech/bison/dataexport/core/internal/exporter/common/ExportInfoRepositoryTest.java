/*-
 * ========================LICENSE_START=================================
 * datalift
 * ========================================================================
 * Copyright (C) 2026 Bison Schweiz AG
 * ========================================================================
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
 * =========================LICENSE_END==================================
 */
package tech.bison.dataexport.core.internal.exporter.common;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import io.vrap.rmf.base.client.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.bison.dataexport.core.api.configuration.Configuration;
import tech.bison.dataexport.core.api.executor.Context;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ExportInfoRepositoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProjectApiRoot projectApiRoot;
    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Configuration configuration;

    @BeforeEach
    void setup() {
        when(configuration.getApiRoot()).thenReturn(projectApiRoot);
    }

    @Test
    void getCurrent_customObjectExists_returnCurrentInfo() {
        var lastExportTimestamp = ZonedDateTime.parse("2026-02-01T10:00:00Z");
        var existingExportInfo = new ExportInfo(Map.of(), 1L);
        var customObject = createCustomObject(existingExportInfo);

        when(projectApiRoot.customObjects()
                .withContainerAndKey("dataExport", "dataExportKey")
                .get()
                .executeBlocking()
                .getBody())
                .thenReturn(customObject);
        when(objectMapper.convertValue(
                eq(customObject.getValue()),
                Mockito.<TypeReference<Map<String, ZonedDateTime>>>any()))
                .thenReturn(Map.of("key", lastExportTimestamp));

        var exportInfo = createExportInfoRepository().getCurrent(createContext());

        assertThat(exportInfo.exportTimestamps()).isEqualTo(Map.of("key", lastExportTimestamp));
    }


    @Test
    void getCurrent_customObjectNotExists_returnEmptyExportInfo() {
        when(projectApiRoot.customObjects()
                .withContainerAndKey("dataExport", "dataExportKey")
                .get()
                .executeBlocking()
                .getBody()).thenThrow(NotFoundException.class);

        var exportInfo = createExportInfoRepository().getCurrent(createContext());

        assertThat(exportInfo.exportTimestamps()).isEqualTo(Map.of());
    }

    @Test
    void update_createOrUpdateCustomObject() {
        var lastExportTimestamp = ZonedDateTime.parse("2026-02-01T10:00:00Z");
        var customObject = createCustomObject(new ExportInfo(Map.of("key", ZonedDateTime.parse("2026-01-31T10:00:00Z")), 2L));
        when(projectApiRoot.customObjects()
                .post(Mockito.any(CustomObjectDraft.class))
                .executeBlocking()
                .getBody()).thenReturn(customObject);

        var exportInfo = createExportInfoRepository().update(createContext(), Map.of("key", lastExportTimestamp), 1L);

        assertThat(exportInfo.exportTimestamps()).isEqualTo(Map.of("key", lastExportTimestamp));
        assertThat(exportInfo.documentVersion()).isEqualTo(2L);
    }

    private Context createContext() {
        return new Context(configuration);
    }

    private CustomObject createCustomObject(ExportInfo exportInfo) {
        var customObject = mock(CustomObject.class);
        lenient().when(customObject.getValue()).thenReturn(exportInfo.exportTimestamps());
        lenient().when(customObject.getVersion()).thenReturn(exportInfo.documentVersion());
        return customObject;
    }

    private ExportInfoRepository createExportInfoRepository() {
        return new ExportInfoRepository(objectMapper);
    }
}

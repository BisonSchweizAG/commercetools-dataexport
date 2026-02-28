package tech.bison.dataexport.core.internal.exporter.common;

import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.error.NotFoundException;
import tech.bison.dataexport.core.api.executor.Context;

import java.time.ZonedDateTime;
import java.util.Map;

public class DeltaExportInfoRepository {
    private final ObjectMapper objectMapper;
    private static final String EXPORT_CONTAINER = "dataExport";
    private static final String EXPORT_OBJECT_KEY = "dataExportKey";

    public DeltaExportInfoRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DeltaExportInfo getCurrent(Context context) {
        try {
            var responseBody = context.getProjectApiRoot().customObjects()
                    .withContainerAndKey(EXPORT_CONTAINER, EXPORT_OBJECT_KEY).get()
                    .executeBlocking()
                    .getBody();
            var exportTimeStamps = objectMapper.convertValue(responseBody.getValue(), new TypeReference<Map<String, ZonedDateTime>>() {
            });
            return new DeltaExportInfo(exportTimeStamps, responseBody.getVersion());
        } catch (NotFoundException ex) {
            return new DeltaExportInfo(Map.of(), null);
        }
    }

    public DeltaExportInfo update(Context context, Map<String, ZonedDateTime> exportTimeStamps, Long documentVersion) {
        var draft = CustomObjectDraft.builder()
                .container(EXPORT_CONTAINER)
                .key(EXPORT_OBJECT_KEY)
                .version(documentVersion)
                .value(exportTimeStamps)
                .build();
        var updateResponse = context.getProjectApiRoot().customObjects().post(draft).executeBlocking();
        return new DeltaExportInfo(exportTimeStamps, updateResponse.getBody().getVersion());
    }
}
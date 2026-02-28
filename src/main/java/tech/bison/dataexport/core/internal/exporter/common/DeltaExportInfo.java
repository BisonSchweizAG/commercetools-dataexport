package tech.bison.dataexport.core.internal.exporter.common;

import java.time.ZonedDateTime;
import java.util.Map;

public record DeltaExportInfo(Map<String, ZonedDateTime> exportTimestamps, Long documentVersion) {
}

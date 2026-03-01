package tech.bison.dataexport.core.internal.exporter.common;

import java.time.ZonedDateTime;
import java.util.Map;

public record ExportInfo(Map<String, ZonedDateTime> exportTimestamps, Long documentVersion) {
}

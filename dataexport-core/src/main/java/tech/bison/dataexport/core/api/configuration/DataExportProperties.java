package tech.bison.dataexport.core.api.configuration;

import tech.bison.dataexport.core.api.executor.ExportableResourceType;

import java.util.List;

public record DataExportProperties(ExportableResourceType resourceType, List<String> fields) {
}

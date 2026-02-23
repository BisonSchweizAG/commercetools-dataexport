package tech.bison.dataexport.core.api.executor;

import tech.bison.dataexport.core.api.configuration.DataExportProperties;

import java.io.OutputStream;

@FunctionalInterface
public interface DataWriterProvider {

    DataWriter create(DataExportProperties dataExportProperties, OutputStream outputStream);
}

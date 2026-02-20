package tech.bison.dataexport.core.api.executor;

import java.io.OutputStream;
import tech.bison.dataexport.core.api.configuration.DataExportProperties;

@FunctionalInterface
public interface DataWriterProvider {

  DataWriter create(DataExportProperties dataExportProperties, OutputStream outputStream);
}

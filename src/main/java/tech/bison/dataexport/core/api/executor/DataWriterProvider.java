package tech.bison.dataexport.core.api.executor;

import java.io.OutputStream;
import java.util.List;

@FunctionalInterface
public interface DataWriterProvider {

    DataWriter create(List<String> fields, OutputStream outputStream);
}

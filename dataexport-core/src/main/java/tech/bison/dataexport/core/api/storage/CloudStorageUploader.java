package tech.bison.dataexport.core.api.storage;

import tech.bison.dataexport.core.api.command.ResourceExportData;

public interface CloudStorageUploader {
    void upload(ResourceExportData resourceExportData);
}

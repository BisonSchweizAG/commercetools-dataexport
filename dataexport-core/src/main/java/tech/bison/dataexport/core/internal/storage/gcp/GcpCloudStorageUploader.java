package tech.bison.dataexport.core.internal.storage.gcp;

import tech.bison.dataexport.core.api.command.ResourceExportData;
import tech.bison.dataexport.core.api.configuration.GcpCloudStorageProperties;
import tech.bison.dataexport.core.api.storage.CloudStorageUploader;

public class GcpCloudStorageUploader implements CloudStorageUploader {
    private final GcpCloudStorageProperties gcpCloudStorageProperties;

    public GcpCloudStorageUploader(GcpCloudStorageProperties gcpCloudStorageProperties) {
        this.gcpCloudStorageProperties = gcpCloudStorageProperties;
    }

    @Override
    public void upload(ResourceExportData resourceExportData) {

    }
}

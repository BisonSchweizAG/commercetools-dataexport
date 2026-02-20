# commercetools Dataexport

With commercetools Data Export you can export data from commercetools to csv files and upload them to a cloud storage.
The exported data is always a full export of the corresponding resource type.

The following commercetools resource types are supported:

- Orders

The following cloud storage types are supported:

- Google Cloud Storage

## Usage

### 1. Add dependency

Add the data cleanup core module to your gradle or maven file.

```groovy
implementation "tech.bison:commercetools-dataexport:x.y.z"
```

(latest version numbers avaible
on [Maven Central](https://central.sonatype.com/search?namespace=tech.bison&name=commercetools-dataexport))

### 2. Configure the export

```java
DataExport dataExport = DataExport.configure()
    .withApiRoot(projectApiRoot)
    .withExportFields(ExportableResourceType.ORDER,
        List.of("id", "orderNumber", "createdAt", "customerId", "totalPrice", "lineItems.id"))
    .withGcpCloudStorageProperties(new GcpCloudStorageProperties("gcpProjectId", "bucketName", null))
    .load();
```

Fields can be configured with the dot notation according to the commercetools api documentation.

The money type centPrecision can be configured with a short hand notation by just referring to the parent field name.
The exported value will be the centAmount divided by 100.
Example:

```
DataExport.configure()
    .withExportFields(ExportableResourceType.ORDER, List.of("id", "totalPrice","lineItems.taxedPrice.totalNet")
```

Some resource types support child items. Child items are added to the csv file below the parent item. For child item
lines all parent field values will be empty. <br>Child item fields can be configured with the dot notation:

- order: lineItems. Example: lineItems.id

## Building

There is a possibility to use alternative url to maven central:
create gradle.properties and set for example:
REPO1_URL=https://artifactory.example.com/repo1

## License

commercetools Data Export is published under the Apache License 2.0, see http://www.apache.org/licenses/LICENSE-2.0 for
details.

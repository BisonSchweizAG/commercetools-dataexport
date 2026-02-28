# commercetools Dataexport

With commercetools Data Export you can export data from commercetools to csv files and upload them to an upload
destination.
The exported data is always a full export of the corresponding resource type.

The following commercetools resource types are supported:

- Orders
- Customers

The following upload destinations are supported:

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
        .withOrderExport(List.of("id", "orderNumber", "createdAt", "customerId", "totalPrice", "lineItems.id"), ExportMode.FULL)
        .withCustomerExport(List.of("id", "firstname", "lastName"))
        .withGcpCloudStorageProperties(new GcpCloudStorageProperties("gcpProjectId", "bucketName", null))
        .load();
```

The configuration above will export all orders to the gcp cloud storage bucket in the following structure:
/orders/orders_YYYY_MM_DD_HH_mm_ss.csv

Fields can be configured with the dot notation according to the commercetools api documentation.

The money type centPrecision can be configured with a short hand notation by just referring to the parent field name.
The exported value will be the centAmount divided by fraction digits.
Example:

```java
DataExport dataExport = DataExport.configure()
        // ...
        .withOrderExport(List.of("id", "totalPrice", "lineItems.taxedPrice.totalNet"), ExportMode.FULL)
        .load();
```

## Configuration options

### Export child items

Some resource types support child items. Child items are added to the csv file below the parent item. For child item
lines all parent field values will be empty. <br>Child item fields can be configured with the dot notation:

| Resource Type | Child Items | Example      |
|---------------|-------------|--------------|
| order         | lineItems   | lineItems.id |
| customer      | addresses   | addresses.id |

Special case for order line item variant attributes:

- Attribute value by name: `lineItems.variant.attributes.<attributeName>`
- Nested value (for example expanded references): `lineItems.variant.attributes.<attributeName>.<nestedPath>`

Examples:

- `lineItems.variant.attributes.color`
- `lineItems.variant.attributes.supplierCategory.obj.name`

### Limit number of records in the upload

The uploaded files can be chunked by the number of records. If the max records per upload is reached a new file will be
created.
Example:

```java
DataExport dataExport = DataExport.configure()
        // ...
        .withMaxRecordsPerUpload(10000)
        .load();
```

### Full and delta export

The built-in resource types do a full export by default. Additionally, the orders resource type supports delta exports.
Pass the ExportMode.DELTA to the export configuration to enable it.

### Register a custom exporter

If you want full control of the export logic you can configure a custom exporter. Just implement the DataExporter and
DataWriter interfaces and configure it as follows:

```java
DataExport dataExport = DataExport.configure()
        // ...
        .withCustomExporter("someKey", new CustomExporter(), (fields, outputStream) -> new CustomDataWriter(outputStream))
        .load();
```

The key will be used as a prefix for the exported files.

## Building

There is a possibility to use alternative url to maven central:
create gradle.properties and set for example:
REPO1_URL=https://artifactory.example.com/repo1

## License

commercetools Data Export is published under the Apache License 2.0, see http://www.apache.org/licenses/LICENSE-2.0 for
details.

# commercetools Dataexport

With commercetools Data Export you can export data from commercetools to a cloud storage. 

## Usage with Spring Boot

### 1. Add dependency

Add our Spring Boot Starter to your gradle or maven file.

```groovy
implementation "tech.bison:commercetools-dataexport-spring-boot-starter:x.y.z"
```

(latest version numbers avaible on [Maven Central](https://central.sonatype.com/search?namespace=tech.bison&name=commercetools-dataexport-spring-boot-starter))

### 2. Configuration

Use application properties to configure the cleanup predicates for the commercetools resources to cleanup:

```yaml
TODO

```

### 3. Create a background cleanup job

Take your background job library of your choice and execute the data export with the Core API.
Example with Spring Scheduling and [ShedLock](https://github.com/lukas-krecan/ShedLock):

```java
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

//...

@AutoWired
public ExportJob(DataExport dataExport) {
    this.dataExport = dataExport;
}

@Scheduled(cron = "0 2 * * *")
@SchedulerLock(name = "export")
public void scheduledTask() {
    LockAssert.assertLocked();
    dataExport.execute();
}
```

## Usage with the Core API

### 1. Add dependency

Add the data cleanup core module to your gradle or maven file.

```groovy
implementation "tech.bison:commercetools-dataexport-core:x.y.z"
```

(latest version numbers avaible on [Maven Central](https://central.sonatype.com/search?namespace=tech.bison&name=commercetools-dataexport-core))

### 2. Configure and execute the cleanup commands

```java
DataExport dataExport = DataExport.configure()
        .withApiProperties(new CommercetoolsProperties("clientId", "clientSecret", "apiUrl", "authUrl", "projectKey"))
        .load()
        .execute();
```

## Building

There is a possibility to use alternative url to maven central:
create gradle.properties and set for example:
REPO1_URL=https://artifactory.example.com/repo1

## License

commercetools Data Cleanup is published under the Apache License 2.0, see http://www.apache.org/licenses/LICENSE-2.0 for details.

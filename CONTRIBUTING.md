# Contributing

Contributions are very welcome. The following will provide some helpful guidelines.

## How to build the project

DataExport requires at least JDK 25 to build.

```
$ cd /path/to/git/clone/of/DataExport
$ ./gradlew build
```

## How to contribute

If you want to tackle an existing issue please add a comment to make sure the issue is sufficiently discussed
and that no two contributors collide by working on the same issue.
To submit a contribution, please follow the following workflow:

* Fork the project
* Create a feature branch
* Add your contribution
* When you're completely done, build the project and run all tests via `./gradlew clean build`
* Create a Pull Request

### Commits

Commit messages should be clear and fully elaborate the context and the reason of a change.
Each commit message should follow the following conventions:

* it must start with a title
    * less than 72 characters
    * starting lowercase
    * use the imperative style
* use the body to explain what and why you have done something
    * separated from the title by a blank line
* if your commit refers to an issue, please prefix the title with the issue number, e.g. `#123`

A full example:

```
 #123 add helper method for JSON-based migrations

The easiest way to support JSON-based migrations is to provide a helper method to read from a JSON-file. 
Then one can used this method in a Java-based migration and post the body to the correct endpoint via commercetools api root object.
```

Furthermore, commits must be signed off according to the [DCO](DCO).

### Pull Requests

If your Pull Request resolves an issue, please add a respective line to the end, like

```
Resolves #123
```

### Formatting

Please adjust your code formatter to the general style of the project. To help with this, there is a *.editorconfig* file in the root folder of the project.

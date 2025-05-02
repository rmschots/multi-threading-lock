multi-threading-lock-before [![Maven Central](https://img.shields.io/maven-central/v/com.rmschots.maven-plugins/multi-threading-lock-before)](https://central.sonatype.com/artifact/com.rmschots.maven-plugins/multi-threading-lock-before)

multi-threading-lock-after [![Maven Central](https://img.shields.io/maven-central/v/com.rmschots.maven-plugins/multi-threading-lock-after)](https://central.sonatype.com/artifact/com.rmschots.maven-plugins/multi-threading-lock-after)

# Multi-Threading Lock

## Overview

This project provides a Maven plugin for managing multi-threading locks in Maven builds. It simplifies the process of
handling locks, ensuring that resources are accessed in a thread-safe manner.

The inspiration for this plugin came from the problems arising from running protocol buffer builds with shared resources
in parallel.
The plugin addresses these issues by providing a mechanism to lock resources during the build process.

## Features

- **Automatic Lock Management**: Automatically acquires and releases locks as needed using a file-based locking
  mechanism.
- **Configurable Options**: Customize lock behavior through Maven configuration.
- **Performance Optimization**: Designed to minimize performance overhead while ensuring thread safety.

## Usage

To use this plugin, include it in your `pom.xml` file:

```xml

<plugins>
    <plugin>
        <groupId>com.rmschots.maven-plugins</groupId>
        <artifactId>multi-threading-lock-before</artifactId>
        <version>1.0.0</version>
        <executions>
            <execution>
                <phase>generate-sources</phase>
                <goals>
                    <goal>lock</goal>
                </goals>
                <configuration>
                    <lockName>custom-lock-name</lockName> <!-- required -->
                    <timeout>60000</timeout> <!-- optional, default 300000 -->
                    <lockDir>customDir</lockDir> <!-- optional, default ${java.io.tmpdir}/maven-locks -->
                </configuration>
            </execution>
        </executions>
    </plugin>
    <plugin>
        ... <!-- a plugin that should not be executed in parallel -->
    </plugin>
    <plugin>
        <groupId>com.rmschots.maven-plugins</groupId>
        <artifactId>multi-threading-lock-after</artifactId>
        <version>1.0.0</version>
        <executions>
            <execution>
                <phase>generate-sources</phase>
                <goals>
                    <goal>unlock</goal>
                </goals>
                <configuration>
                    <lockName>custom-lock-name</lockName> <!-- required -->
                    <lockDir>customDir</lockDir> <!-- optional, default ${java.io.tmpdir}/maven-locks -->
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE.md) file for details.

---
layout: page
title:  "Cli Application"
section: "cli"
position: 3
---

# Build Cli application

Best way to use cli application is to build it using [graal-vm](https://www.graalvm.org/docs/getting-started/).

To do it:

* Download and install [graal-vm](https://www.graalvm.org/docs/getting-started/)
* Run in console `sbt "hocones-cli/graalvm-native-image:packageBin"`
* You can find binary in `hocones-cli/target/graalvm-native-image`
* And have fun :)

## How to use cli

To generate full documentation - like in sbt application just run `./hocones-cli <path-to-configuration-file>`

But you can also run single commands of application:

### Generating [environment file](https://docs.docker.com/compose/env-file/)

Usage:

`./hocones-cli env-file --output <path-where-to-save-environment-file> <path-to-configuration-file>`

Options:
* `-o` or `--output` define output file
* `-c` or `--comments` enable printing comments in environment file. Comments contain information like path from configuration, default value, path to file, description from meta file etc.
* `-d` or `--defaults` should default values be used in generation of environment file
* `-r` or `--remove-duplicates` should environment file be cleared from duplicates of keys

Examples:

* `./hocones-cli env-file --output ./my-project-environments.env --comments --defaults --remove-duplicates my-project/src/main/resources/application.conf`
* `./hocones-cli env-file -o ./my-project-environments.env -c -d -r my-project/src/main/resources/application.conf`

### Generating [markdown](https://en.wikipedia.org/wiki/Markdown) file with environment files 

Usage:

`./hocones-cli env-docs --output <path-where-to-save-environment-file> <path-to-configuration-file>`

Options:

* `-o` or `--output` define output file
* `--a <alignment>` or `--alignment <alignment>` alignment of values in markdown table (left, right, center)

Examples:

* `./hocones-cli env-file --output ./env.md --alignment center my-project/src/main/resources/application.conf`
* `./hocones-cli env-file -o ./env.md -a center my-project/src/main/resources/application.conf`

### Generating [markdown](https://en.wikipedia.org/wiki/Markdown) file with documentation for whole configuration

Usage:

`./hocones-cli docs --output <path-where-to-save-environment-file> <path-to-configuration-file>`

Options:

* `-o` or `--output` define output file

Examples:

* `./hocones-cli docs --output ./docs.md my-project/src/main/resources/application.conf`
* `./hocones-cli docs -o ./docs.md my-project/src/main/resources/application.conf`

### Statistics

Usage:

`./hocones-cli statistics <path-to-configuration-file>`

Display statistics about configuration. Number of environment variables, references etc.
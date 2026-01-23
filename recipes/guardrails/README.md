## Gradle Guard

Gradle Guard is a sort of "linter" that parses Gradle Kotlin DSL scripts (anything with the `.gradle.kts` extension) and
provides a report of any element ("statement") that is not explicitly allow-listed. This is useful for keeping your 
build scripts declarative, and therefore helps with maintainability and likely also performance (by preventing 
regressions).

### Usage

**Create a simple config file**

The file name can be anything but the extension should be either `.yml` or `.yaml`. We suggest `gradle-guard.yml`.

Sample contents:

```yaml
# Globally-allowed blocks (by name)
allowed_blocks:
  - "plugins"
  - "dependencies"
  - "myCustomExtension"

# Globally-allowed statements, matched by prefix.
# "tasks." would match "tasks.jar", "tasks.named", etc.
allowed_prefixes:
  - "tasks."
  - "val javaTarget ="

# List of paths to ignore. Can be specific files or directories, or a mix.
# Directories will include all sub-paths recursively. Trailing `/` is optional.
# They should be relative to the working directory of the tool, typically the repo root.
ignored_paths:
  - "a/b/build.gradle.kts"
  - "a/"
  - "b/c"
```

**Help**

`gradle-guard` has two commands, `check` and `baseline`. Any of the following can be used to see usage details. Omitting
either `-h` or `--help` is treated as if requesting usage details (that is, those options aren't strictly necessary).

```shell
gradle-guard [-h|--help]
gradle-guard check [-h|--help]
gradle-guard baseline [-h|--help]
```

#### Checking your scripts

**Generate a human-readable report:**

```shell
gradle-guard check --config=gradle-guard.yml .
```

This will generate a human-readable report. It might look like this:

```shell
Analysis complete. Found:
- 0 without any violations.
- 12 with violations.

The build script 'foo/bar/build.gradle.kts' contains 3 forbidden statements:

1: gradlePlugin { … (named block)
   Start: (Line: 10, Column: 0)
   End:   (Line: 29, Column: 0)

2: kotlin { … (named block)
   Start: (Line: 31, Column: 0)
   End:   (Line: 33, Column: 0)
   
3: val javaTarget = JavaLanguageVersion.of(libs.versions.java.get()) … (declaration)
   Start: (Line: 46, Column: 0)
   End:   (Line: 46, Column: 65)
```

**Machine-readable output:**

The default output is fairly verbose and aimed at readability. The tool can also generate more concise, machine-readable
output:

```shell
gradle-guard check --config=gradle-guard.yml --format=machine .
```

That output might look like this, for the same input as above:

```shell
foo/bar/build.gradle.kts:10 has forbidden block gradlePlugin { … }
foo/bar/build.gradle.kts:31 has forbidden block kotlin { … }
foo/bar/build.gradle.kts:46 has forbidden declaration val javaTarget = JavaLanguageVersion.of(libs.versions.java.get()) …
````

> Note. The `--format` option supports three values:
> 1. `human` (default).
> 2. `machine`.
> 3. `computer` (an alias for `machine`).

**If the report is not empty, the return value from invoking the CLI in this case is non-zero.** 

#### Generating a baseline for your scripts

To support incremental, ratchet-style improvements, the tool has baseline functionality.

```shell
gradle-guard baseline --config=gradle-guard.yml --output=baseline.yml .
```

This will generate a `baseline.yml` file with the same format as `gradle-guard.yml`, which explicitly allow-lists all
"forbidden" statements that _would_ have been errors according to the `check` command. That file might look like this:

```shell
allowed_blocks:
  - "plugins"
  - "dependencies"
baseline:
- path: "foo/bar/build.gradle.kts"
  allowed_blocks:
  - "gradlePlugin"
  - "kotlin"
  allowed_prefixes:
  - "val javaTarget = JavaLanguageVersion.of(libs.versions.java.get())"
```

This baseline file will include the contents of the file passed to the `--config` argument, and therefore could replace
that file to simplify workflows.

**Using the baseline**

Once a baseline file exists, it can be used with both the `check` and `baseline` commands.

```shell
gradle-guard check    --config=gradle-guard.yml --baseline=baseline.yml .                           # (1)
gradle-guard baseline --config=gradle-guard.yml --baseline=baseline.yml .                           # (2)
gradle-guard baseline --config=gradle-guard.yml --baseline=baseline.yml --output=new-baseline.yml . # (3)
```

1. When used with `check`, the functionality combines the two config files (`gradle-guard.yml` and `baseline.yml`) when
   generating reports.
2. When used with `baseline` _without_ the `--output` option, will generate a new baseline, overwriting the original.
3. When used with `baseline` _with_ the `--output` option, will generate a new baseline at the specified location. This
   new baseline is _not_ incremental. It is suitable as a full replacement for the original baseline.

## Contributing

### Run it: Dev mode

```shell
./gradlew recipes:guardrails:gradle-guard:run --args="..."
```

### Run it: As a final binary

#### Using Shadow

**First, build the uber jar**

```shell
./gradlew recipes:guardrails:gradle-guard:shadowJar
```

**Now we can execute our uber jar directly with `java`**

```shell
java -jar recipes/guardrails/gradle-guard/build/libs/gradle-guard-0.1-SNAPSHOT-all.jar --help
```

#### Using the Distribution plugin

```shell
# 1. Install dist
./gradlew recipes:guardrails:gradle-guard:installDist

# 2. Run binary
./recipes/guardrails/gradle-guard/build/install/gradle-guard/bin/gradle-guard --config=... --baseline=... <path>
```

> Note. There is a sample config file at `recipes/guardrails/gradle-guard/src/test/resources/gradle-guard.yml`.

## Test it

```shell
./gradlew -p recipes/guardrails check
```

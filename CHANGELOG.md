# KotlinEditor

## Version 0.19
* [feat]: added new public methods in `Contexts`, and a new `Statements` helper class.

## Version 0.18
* [chore]: update antlr to 4.13.2 (latest).

## Version 0.17
* [fix]: add plugins block below imports, if any.

## Version 0.16
* [feat]: add DependenciesMutator for rewriting dependency strings.

## Version 0.15
* [feat]: add `isChanged()` to avoid rewriting unchanged files.
* [fix]: parameter can be null.
* [fix]: add `@Throws(IllegalStateException::class)` on `DependenciesSimplifier` factories.
* [fix]: check size before calling `single()`. Declare `IllegalArgumentException` too.
* [fix]: be stricter about what is a dependencies block we can parse.
* [fix]: handle project declaration without named arguments.

## Version 0.14
* [feat]: support parsing more complex dependency declarations.
* [feat]: add `DependenciesSimplifier` recipe.

## Version 0.13
* [fix]: maintain terminal newline in `CommentsInBlockRemover`.

## Version 0.12
* [fix]: make `CommentsInBlockRemover` expose found removable comments.

## Version 0.11
* [feat]: add a utility class to remove comments from a block in a build script.

## Version 0.10
* [feat]: expose `StatementContext` with parsed dependency declarations

## Version 0.9
* [feat]: DependencyExtractor includes all statements in DependencyContainer.

## Version 0.8
* [fix]: handle more kinds of dependency declarations on properties.

## Version 0.7
* [fix]: handle dependency declarations on properties.

## Version 0.6q
* [feat] support enforcedPlatform as a dependency capability.

## Version 0.5
* [feat] support parsing `gradleApi()`-like dependency declarations.
* [fix] improve error messages during parse errors.

## Version 0.4
* [feat] improve support for parsing dependencies.

## Version 0.3
* [feat] smarter indentation detection.
* [feat] simplify terminal newline calculation.
* [fix] fix trailing newline issue for kotlinFile.
* [fix] rename exception to be more generic.

## Version 0.2
* [feat] add support for terminal new lines in KotlinFileContext.

## Version 0.1

First OSS release.

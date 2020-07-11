# Automated Tab Organization - Heuristics Engine

This repository contains the Heuristics Engine for my master thesis on `Automated Tab Grouping` at the `University of Zurich` (in its early stages).

The Scala application in this repository can only be used in conjunction with the corresponding WebExtension, which is available from a separate source. Once installed, the Heuristics Engine computes several heuristics based on tab interactions collected from browsers and, based on these interactions, proposes tab groups that might be of interest to the user.

To ensure privacy of the potentially sensitive browsing data, all storage and processing is performed on the local machine and nothing is ever shared across a network. For the purposes of experiments, selected data will be exported by experiment participants and can be reviewed before submitting it for further analysis.

For feature requests, bug reports, and feedback of any kind, please refer to the following form:
<https://docs.google.com/forms/d/1pLqMgDVgzzf7yAmn6JLd0jJbrn3QlFZNVaYlKWVie78/edit>

## Requirements

- JDK 1.8 or 11
  - https://adoptopenjdk.net/
- Scala SBT

## Development

Install all dependencies

> `sbt update`

Create a staging build of the heuristics engine:

> `sbt stage`

Create a package for Linux:

> `./_build-linux.sh`

Create a package for MacOS:

> `./_build-osx.sh`

### Windows Deployment

1. Generate a new `tabs.wxs` configuration using `sbt windows:wixFile`
2. Replace `<Component` with `<Component Win64="yes"` (Whole Word!)
3. Replace `<Package` with `<Package Platform="x64"` (Whole Word!)
4. Replace `ProgramFilesFolder` with `ProgramFiles64Folder`
5. Remove unnecessary components and featurs (shortcut, path)
6. Add custom component for registry entries from `winpkg.wxs`
7. Move the updated `tabs.wxs` to `target/windows/package.wxs`
8. Package the binary using `sbt windows:packageBin`

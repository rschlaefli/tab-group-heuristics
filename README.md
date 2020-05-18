# Automated Tab Organization - Heuristics Engine

This repository contains the Heuristics Engine for my master thesis on `Automated Tab Grouping` at the `University of Zurich` (in its early stages).

The Scala application in this repository can only be used in conjunction with the corresponding WebExtension, which is available from a separate source. Once installed, the Heuristics Engine computes several heuristics based on tab interactions collected from browsers and, based on these interactions, proposes tab groups that might be of interest to the user.

To ensure privacy of the potentially sensitive browsing data, all storage and processing is performed on the local machine and nothing is ever shared across a network. For the purposes of experiments, selected data will be exported by experiment participants and can be reviewed before submitting it for further analysis.

For feature requests, bug reports, and feedback of any kind, please refer to the following form:
<https://docs.google.com/forms/d/1pLqMgDVgzzf7yAmn6JLd0jJbrn3QlFZNVaYlKWVie78/edit>

## Requirements

- Java 1.8
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

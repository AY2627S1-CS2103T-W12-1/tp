---
layout: page
title: Logging guide
---

TrackCall uses `java.util.logging`. `LogsCenter` manages logging levels and destinations.

## Obtain a logger

Call `LogsCenter.getLogger(Class)` to obtain a class logger.
It logs messages at the configured level.

## Find log output

Messages go to the console and a `.log` file.
If the file handler cannot start, console logging remains available.

## Choose a logging level

Change the `LOG_LEVEL` constant in `LogsCenter` to control the output level.
The default is `INFO`.

When choosing a level for a message, follow the
[Java logging conventions](https://se-education.org/guides/conventions/java/logging.html).

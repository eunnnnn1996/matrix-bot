#!/usr/bin/env sh
# Helper wrapper script (POSIX) - expects gradle wrapper files to exist or run setup-wrapper.ps1 on Windows.
# If Gradle installed system-wide, `gradle` will be used.
if command -v gradle >/dev/null 2>&1; then
  gradle "$@"
else
  echo "Gradle not found. On Windows run setup-wrapper.ps1 once to fetch the Gradle wrapper, or install Gradle."
  exit 1
fi

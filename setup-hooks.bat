@echo off
echo Setting up git hooks...

:: Create hooks directory if it doesn't exist
if not exist .git\hooks mkdir .git\hooks

:: Create pre-commit hook
(
echo #!/bin/sh
echo echo "Running pre-commit hooks..."
echo.
echo # Use the appropriate Gradle wrapper based on the OS
echo if [ "$OSTYPE" = "msys" ] ^|^| [ "$OSTYPE" = "win32" ]; then
echo     ./gradlew.bat ktlintFormat detekt ^|^| exit 1
echo else
echo     ./gradlew ktlintFormat detekt ^|^| exit 1
echo fi
) > .git\hooks\pre-commit

:: Create pre-push hook
(
echo #!/bin/sh
echo echo "Running pre-push hooks..."
echo.
echo # Use the appropriate Gradle wrapper based on the OS
echo if [ "$OSTYPE" = "msys" ] ^|^| [ "$OSTYPE" = "win32" ]; then
echo     ./gradlew.bat checkCodeStyle ^|^| exit 1
echo else
echo     ./gradlew checkCodeStyle ^|^| exit 1
echo fi
) > .git\hooks\pre-push

echo Git hooks have been set up successfully!
echo.
echo The following hooks are now active:
echo - pre-commit: Runs ktlintFormat and detekt to format and check code before committing
echo - pre-push: Runs full code style checks before pushing
echo.
echo You can skip hooks if needed using: git commit --no-verify

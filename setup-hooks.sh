#!/bin/bash

echo "Setting up git hooks..."

# Create hooks directory if it doesn't exist
mkdir -p .git/hooks

# Create pre-commit hook with platform-independent line endings and Gradle wrapper
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/sh
echo "Running pre-commit hooks..."

# Use the appropriate Gradle wrapper based on the OS
if [ "$OSTYPE" = "msys" ] || [ "$OSTYPE" = "win32" ]; then
    ./gradlew.bat ktlintFormat detekt || exit 1
else
    ./gradlew ktlintFormat detekt || exit 1
fi
EOF

# Create pre-push hook with platform-independent line endings and Gradle wrapper
cat > .git/hooks/pre-push << 'EOF'
#!/bin/sh
echo "Running pre-push hooks..."

# Use the appropriate Gradle wrapper based on the OS
if [ "$OSTYPE" = "msys" ] || [ "$OSTYPE" = "win32" ]; then
    ./gradlew.bat checkCodeStyle || exit 1
else
    ./gradlew checkCodeStyle || exit 1
fi
EOF

# Make hooks executable (this is ignored on Windows but needed for Unix)
chmod +x .git/hooks/pre-commit
chmod +x .git/hooks/pre-push

echo "Git hooks have been set up successfully!"
echo ""
echo "The following hooks are now active:"
echo "- pre-commit: Runs ktlintFormat and detekt to format and check code before committing"
echo "- pre-push: Runs full code style checks before pushing"
echo ""
echo "You can skip hooks if needed using: git commit --no-verify"

#!/bin/zsh
set -euo pipefail

PROJECT_DIR="${0:A:h}"
INSTANCE_DIR="${PROJECT_DIR:h}"
MINECRAFT_JAR="$INSTANCE_DIR/../26.2-Fabric.jar"
JDK25="/Users/xuqiran/Library/Application Support/hmcl/java/macos-x86_64/mojang-java-runtime-epsilon/jre.bundle/Contents/Home"
LIBRARY_ROOT="/Users/xuqiran/Downloads/.minecraft/libraries"
PROCESSED_MODS="$INSTANCE_DIR/../.fabric/processedMods"
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_JAR="$INSTANCE_DIR/directors-cut-1.0.0+mc26.2.jar"

if [[ ! -x "$JDK25/bin/javac" ]]; then
  print -u2 "Java 25 compiler not found at $JDK25"
  exit 1
fi

if [[ ! -f "$MINECRAFT_JAR" ]]; then
  print -u2 "Minecraft 26.2 jar not found at $MINECRAFT_JAR"
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/test-classes"

LIBRARY_CP="$(find "$LIBRARY_ROOT" "$PROCESSED_MODS" -type f -name '*.jar' -print | tr '\n' ':')"
COMPILE_CP="$MINECRAFT_JAR:$LIBRARY_CP"
MAIN_SOURCES=("$PROJECT_DIR"/src/main/java/**/*.java(N))
TEST_SOURCES=("$PROJECT_DIR"/src/test/java/**/*.java(N))

"$JDK25/bin/javac" --release 25 -encoding UTF-8 -classpath "$COMPILE_CP" -d "$BUILD_DIR/classes" "${MAIN_SOURCES[@]}"
cp -R "$PROJECT_DIR/src/main/resources/." "$BUILD_DIR/classes/"

if (( ${#TEST_SOURCES[@]} > 0 )); then
  "$JDK25/bin/javac" --release 25 -encoding UTF-8 -classpath "$COMPILE_CP:$BUILD_DIR/classes" -d "$BUILD_DIR/test-classes" "${TEST_SOURCES[@]}"
  "$JDK25/bin/java" -ea -classpath "$COMPILE_CP:$BUILD_DIR/classes:$BUILD_DIR/test-classes" dev.directorscut.DirectorLogicTest
fi

rm -f "$OUTPUT_JAR"
"$JDK25/bin/jar" --create --file "$OUTPUT_JAR" -C "$BUILD_DIR/classes" .
"$JDK25/bin/jar" --list --file "$OUTPUT_JAR" >/dev/null
print "Built $OUTPUT_JAR"

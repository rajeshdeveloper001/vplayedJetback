#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# build_and_copy_aar.sh
#
# Builds the Android AAR from vplayedKMP using the Kotlin Multiplatform Android
# plugin and copies it to vplayedUpdate/app/libs/shared-release.aar
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PARENT_DIR="$(dirname "$APP_DIR")"
LIBS_DIR="$APP_DIR/app/libs"
mkdir -p "$LIBS_DIR"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Build KMP Android AAR & Copy to app/libs/"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 1. Set JAVA_HOME (JDK 25 required for Gradle 9.6)
AS_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [[ -x "$AS_JBR/bin/java" ]]; then
  export JAVA_HOME="$AS_JBR"
  success "JAVA_HOME set to JDK 25: $JAVA_HOME"
fi

# 2. Find KMP project directory
KMP_DIR=""
if [[ $# -ge 1 && -d "$1" ]]; then
  KMP_DIR="$(cd "$1" && pwd)"
else
  for candidate in "$PARENT_DIR/vplayedKMP" "$PARENT_DIR/kmp" "$PARENT_DIR/vplayed-kmp"; do
    if [[ -d "$candidate" && -f "$candidate/settings.gradle.kts" ]]; then
      KMP_DIR="$(cd "$candidate" && pwd)"
      break
    fi
  done
fi

if [[ -z "$KMP_DIR" ]]; then
  error "vplayedKMP not found. Usage: ./build_and_copy_aar.sh /path/to/vplayedKMP"
  exit 1
fi

success "Found KMP directory: $KMP_DIR"

# 3. Build AAR using maven-publish to the local repo
# In com.android.kotlin.multiplatform.library, publishAllPublicationsToLocalRepoRepository
# deterministically builds and packages the Android release AAR and its metadata.
info "Building Android AAR via KMP publishing task..."
cd "$KMP_DIR"
chmod +x gradlew

# Run the local publication task (or fallback to bundle if available)
JAVA_HOME="$JAVA_HOME" ./gradlew :shared:publishAllPublicationsToLocalRepoRepository --no-daemon

# 4. Find the produced AAR in the output directories
LOCAL_MAVEN="$KMP_DIR/repo/localMaven"
SOURCE_AAR=""

if [[ -d "$LOCAL_MAVEN" ]]; then
  SOURCE_AAR="$(find "$LOCAL_MAVEN" -name "*.aar" | head -1 || true)"
fi

if [[ -z "$SOURCE_AAR" || ! -f "$SOURCE_AAR" ]]; then
  SOURCE_AAR="$(find "$KMP_DIR/shared/build" -name "*.aar" | grep -v "androidTest" | head -1 || true)"
fi

if [[ -n "$SOURCE_AAR" && -f "$SOURCE_AAR" ]]; then
  TARGET_AAR="$LIBS_DIR/shared-release.aar"
  cp -f "$SOURCE_AAR" "$TARGET_AAR"
  success "Copied AAR to: $TARGET_AAR"
  info "File size: $(ls -lh "$TARGET_AAR" | awk '{print $5}')"
else
  error "Could not find assembled AAR in $KMP_DIR"
  exit 1
fi

# 5. Build/Verify App with new AAR
cd "$APP_DIR"
info "Verifying app build with new AAR..."
JAVA_HOME="$JAVA_HOME" ./gradlew :app:assembleDev --no-daemon -x test

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}  SUCCESS: AAR built, copied to app/libs/shared-release.aar, and app verified!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

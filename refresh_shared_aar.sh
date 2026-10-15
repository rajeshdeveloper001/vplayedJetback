#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# refresh_shared_aar.sh
#
# Automated pipeline to:
#   1. Detect KMP repository (vplayedKMP / kmp / vplayed-kmp).
#   2. Compile KMP shared module, generate Compose resources, build Android
#      release AAR and publish with POM metadata to repo/localMaven/.
#   3. Trigger vplayedUpdate Android app to resolve the fresh AAR without stale caching.
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$SCRIPT_DIR"
PARENT_DIR="$(dirname "$APP_DIR")"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  VPlayed KMP -> Local AAR Automated Build & Refresh"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 1. Locate JDK 25 (Android Studio JBR)
AS_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [[ -x "$AS_JBR/bin/java" ]]; then
  export JAVA_HOME="$AS_JBR"
  success "JAVA_HOME set to JDK 25: $JAVA_HOME"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  info "Using existing JAVA_HOME: $JAVA_HOME"
else
  error "JDK 25 required by Gradle 9.6. Please set JAVA_HOME or install Android Studio."
  exit 1
fi

# 2. Locate KMP project directory
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
  error "KMP project not found. Specify path: ./refresh_shared_aar.sh /path/to/vplayedKMP"
  exit 1
fi

success "KMP project located at: $KMP_DIR"

# 3. Publish shared module to repo/localMaven/
info "Building Android release AAR and publishing to repo/localMaven..."
cd "$KMP_DIR"
chmod +x gradlew

# Run publishing task to local repo
JAVA_HOME="$JAVA_HOME" ./gradlew :shared:publishAllPublicationsToLocalRepoRepository --no-daemon --refresh-dependencies

LOCAL_MAVEN="$KMP_DIR/repo/localMaven"
AAR_FILE="$LOCAL_MAVEN/com/vplayed/shared-android/0.1.0/shared-android-0.1.0.aar"

if [[ ! -f "$AAR_FILE" ]]; then
  # Fallback check if artifact name is shared-0.1.0.aar
  AAR_FILE="$(find "$LOCAL_MAVEN" -name "*.aar" | head -1)"
fi

if [[ -f "$AAR_FILE" ]]; then
  success "Published AAR verified: $AAR_FILE ($(ls -lh "$AAR_FILE" | awk '{print $5}'))"
else
  error "Failed to locate generated AAR in $LOCAL_MAVEN"
  exit 1
fi

# 4. Refresh Android App
cd "$APP_DIR"
info "Verifying Android app resolution against new AAR..."
JAVA_HOME="$JAVA_HOME" ./gradlew :app:assembleDev --refresh-dependencies --no-daemon -x test

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}  SUCCESS: Shared AAR generated, published, and linked to app!${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

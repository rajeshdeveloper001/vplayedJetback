#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# setup_kmp.sh – One-shot dev environment bootstrap for vplayedUpdate
#
# What this script does:
#   1. Detects the KMP repo location (vplayedKMP / kmp / vplayed-kmp as siblings,
#      or any path passed as $1 or via VPLAYED_KMP_DIR env var).
#   2. If the KMP folder isn't at ../kmp or ../vplayedKMP already, writes
#      vplayed.kmpDir into ~/.gradle/gradle.properties so Gradle always finds it.
#   3. Sets JAVA_HOME to the Android Studio JBR (JDK 25 – required by Gradle 9.6).
#   4. Updates local.properties with sdk.dir if ANDROID_HOME is available.
#   5. Runs a quick Gradle help task to prove the composite build resolves.
#
# Usage:
#   chmod +x setup_kmp.sh
#   ./setup_kmp.sh                             # auto-discover vplayedKMP
#   ./setup_kmp.sh /path/to/kmp-repo           # explicit path
#   VPLAYED_KMP_DIR=/path/to/kmp ./setup_kmp.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Colours ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ── 0. Locate this script's directory (the Android project root) ──────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_PROJECT="$SCRIPT_DIR"
PARENT_DIR="$(dirname "$ANDROID_PROJECT")"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  VPlayed KMP Setup Script"
echo "  Android project : $ANDROID_PROJECT"
echo "  Sibling dir     : $PARENT_DIR"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# ── 1. Find the vplayedKMP repository ─────────────────────────────────────────
KMP_REPO=""

# Helper: check if a path looks like a Gradle project with :shared
is_kmp_repo() {
  local dir="$1"
  [[ -d "$dir" ]] && \
  [[ -f "$dir/settings.gradle.kts" || -f "$dir/settings.gradle" ]]
}

# (a) Explicit argument or environment variable
if [[ $# -ge 1 && -n "$1" ]]; then
  if is_kmp_repo "$1"; then
    KMP_REPO="$(cd "$1" && pwd)"
    info "Using path from argument: $KMP_REPO"
  else
    error "Provided path '$1' is not a valid Gradle project directory."
    exit 1
  fi
elif [[ -n "${VPLAYED_KMP_DIR:-}" ]]; then
  if is_kmp_repo "$VPLAYED_KMP_DIR"; then
    KMP_REPO="$(cd "$VPLAYED_KMP_DIR" && pwd)"
    info "Using VPLAYED_KMP_DIR: $KMP_REPO"
  else
    error "VPLAYED_KMP_DIR '$VPLAYED_KMP_DIR' is not a valid Gradle project."
    exit 1
  fi
fi

# (b) Auto-discover sibling directories and common locations
if [[ -z "$KMP_REPO" ]]; then
  CANDIDATE_NAMES=("kmp" "vplayedKMP" "vplayed-kmp" "VplayedKMP" "Vplayed-KMP")
  SEARCH_ROOTS=(
    "$PARENT_DIR"
    "$HOME/Desktop"
    "$HOME/Downloads"
    "$HOME/Documents/projects"
    "$HOME/Projects"
    "$HOME/dev"
    "$HOME/workspace"
    "$HOME"
  )
  for root in "${SEARCH_ROOTS[@]}"; do
    for name in "${CANDIDATE_NAMES[@]}"; do
      candidate="$root/$name"
      if is_kmp_repo "$candidate"; then
        KMP_REPO="$(cd "$candidate" && pwd)"
        info "Auto-discovered KMP repo at: $KMP_REPO"
        break 2
      fi
    done
  done
fi

if [[ -z "$KMP_REPO" ]]; then
  error "Could not find the vplayedKMP repository."
  error ""
  error "Please either:"
  error "  1. Run: ./setup_kmp.sh /absolute/path/to/vplayedKMP"
  error "  2. Set env var: VPLAYED_KMP_DIR=/path/to/vplayedKMP ./setup_kmp.sh"
  error "  3. Clone next to this project as 'kmp':"
  error "     git clone <url> $PARENT_DIR/kmp"
  exit 1
fi

success "KMP repo found: $KMP_REPO"

# ── 2. Determine if Gradle will auto-discover it or needs gradle.properties ────
GRADLE_PROPS="$HOME/.gradle/gradle.properties"
NEEDS_GRADLE_PROP=true

# settings.gradle.kts checks ../kmp and ../vplayedKMP natively — no extra config needed
for auto_path in "$PARENT_DIR/kmp" "$PARENT_DIR/vplayedKMP" "$PARENT_DIR/vplayed-kmp"; do
  if [[ -d "$auto_path" ]]; then
    REAL_AUTO="$(cd "$auto_path" && pwd)"
    if [[ "$REAL_AUTO" == "$KMP_REPO" ]]; then
      success "KMP repo is at an auto-discovered path ($auto_path) – no extra Gradle config needed."
      NEEDS_GRADLE_PROP=false
      break
    fi
  fi
done

if $NEEDS_GRADLE_PROP; then
  info "KMP repo is not at a default sibling path – writing vplayed.kmpDir to gradle.properties"
  mkdir -p "$(dirname "$GRADLE_PROPS")"
  # Remove any existing vplayed.kmpDir line, then append the correct one
  if [[ -f "$GRADLE_PROPS" ]]; then
    if grep -q "^vplayed.kmpDir=" "$GRADLE_PROPS"; then
      CURRENT_VAL="$(grep "^vplayed.kmpDir=" "$GRADLE_PROPS" | cut -d= -f2-)"
      if [[ "$CURRENT_VAL" != "$KMP_REPO" ]]; then
        warn "Updating vplayed.kmpDir: $CURRENT_VAL → $KMP_REPO"
        sed -i.bak "s|^vplayed.kmpDir=.*|vplayed.kmpDir=$KMP_REPO|" "$GRADLE_PROPS"
        success "Updated vplayed.kmpDir in $GRADLE_PROPS"
      else
        success "vplayed.kmpDir already correct in $GRADLE_PROPS"
      fi
    else
      echo "" >> "$GRADLE_PROPS"
      echo "# VPlayed KMP composite build – set by setup_kmp.sh" >> "$GRADLE_PROPS"
      echo "vplayed.kmpDir=$KMP_REPO" >> "$GRADLE_PROPS"
      success "Added vplayed.kmpDir=$KMP_REPO to $GRADLE_PROPS"
    fi
  else
    cat > "$GRADLE_PROPS" <<EOF
# Gradle user properties – created by setup_kmp.sh

# VPlayed KMP composite build: path to the vplayedKMP repo
vplayed.kmpDir=$KMP_REPO

# Optional: increase Gradle daemon heap for KMP builds
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
EOF
    success "Created $GRADLE_PROPS with vplayed.kmpDir=$KMP_REPO"
  fi
fi

# ── 3. JAVA_HOME – must be JDK 25 (Android Studio JBR) ───────────────────────
AS_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [[ -x "$AS_JBR/bin/java" ]]; then
  export JAVA_HOME="$AS_JBR"
  success "JAVA_HOME → Android Studio JBR (JDK 25)"
else
  warn "Android Studio JBR not found at: $AS_JBR"
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    warn "Using existing JAVA_HOME: $JAVA_HOME"
  else
    error "JAVA_HOME is not set and Android Studio JBR is missing."
    error "Install Android Studio or export JAVA_HOME to a JDK 25 installation."
    exit 1
  fi
fi
JAVA_VER="$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
info "Java: $JAVA_VER"

# ── 4. local.properties – sdk.dir ─────────────────────────────────────────────
LOCAL_PROPS="$ANDROID_PROJECT/local.properties"
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

if [[ -z "$ANDROID_SDK" ]]; then
  for candidate in \
    "$HOME/Library/Android/sdk" \
    "$HOME/Documents/sdk" \
    "/Users/user/Documents/sdk"; do
    if [[ -d "$candidate" ]]; then
      ANDROID_SDK="$candidate"
      break
    fi
  done
fi

if [[ -n "$ANDROID_SDK" ]]; then
  if [[ -f "$LOCAL_PROPS" ]] && grep -q "^sdk.dir=" "$LOCAL_PROPS"; then
    CURRENT_SDK="$(grep "^sdk.dir=" "$LOCAL_PROPS" | cut -d= -f2-)"
    if [[ "$CURRENT_SDK" == "$ANDROID_SDK" ]]; then
      success "local.properties sdk.dir is already correct."
    else
      warn "Updating sdk.dir: $CURRENT_SDK → $ANDROID_SDK"
      sed -i.bak "s|^sdk.dir=.*|sdk.dir=$ANDROID_SDK|" "$LOCAL_PROPS"
      success "local.properties sdk.dir updated."
    fi
  else
    echo "sdk.dir=$ANDROID_SDK" >> "$LOCAL_PROPS"
    success "Added sdk.dir=$ANDROID_SDK to local.properties."
  fi
else
  warn "Android SDK not found in common paths. Set ANDROID_HOME or update local.properties manually."
fi

# ── 5. gradlew permissions ────────────────────────────────────────────────────
GRADLEW="$ANDROID_PROJECT/gradlew"
chmod +x "$GRADLEW"
success "gradlew is executable."

# ── 6. Verify composite build with Gradle help task ──────────────────────────
echo ""
info "Verifying Gradle composite build (this may download wrappers on first run)..."
echo ""

cd "$ANDROID_PROJECT"

if JAVA_HOME="$JAVA_HOME" "$GRADLEW" help --no-daemon --quiet 2>&1 | tail -3; then
  echo ""
  success "Composite build resolved successfully! ✓"
else
  echo ""
  error "Gradle verification failed. Full output:"
  JAVA_HOME="$JAVA_HOME" "$GRADLEW" help --no-daemon 2>&1 | tail -20 || true
  echo ""
  error "Troubleshooting tips:"
  error "  • Check that $KMP_REPO/settings.gradle.kts includes ':shared'"
  error "  • Make sure both repos share the same toolchain (Gradle 9.6, Kotlin 2.4.21)"
  error "  • Run manually: cd $ANDROID_PROJECT && JAVA_HOME=\"$JAVA_HOME\" ./gradlew help"
  exit 1
fi

# ── 7. Summary ────────────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}  All done! Your dev environment is ready.${NC}"
echo ""
echo "  Persist JAVA_HOME for this terminal session:"
echo -e "  ${YELLOW}export JAVA_HOME=\"$JAVA_HOME\"${NC}"
echo ""
echo "  Or add it permanently to your shell profile (~/.zshrc / ~/.bash_profile):"
echo -e "  ${YELLOW}echo 'export JAVA_HOME=\"/Applications/Android Studio.app/Contents/jbr/Contents/Home\"' >> ~/.zshrc${NC}"
echo ""
echo "  Common build commands (run from $ANDROID_PROJECT):"
echo -e "  ${YELLOW}JAVA_HOME=\"$JAVA_HOME\" ./gradlew assembleDev${NC}         # dev APK"
echo -e "  ${YELLOW}JAVA_HOME=\"$JAVA_HOME\" ./gradlew testDevUnitTest${NC}      # unit tests"
echo -e "  ${YELLOW}JAVA_HOME=\"$JAVA_HOME\" ./gradlew lintDev${NC}              # lint"
echo -e "  ${YELLOW}JAVA_HOME=\"$JAVA_HOME\" ./gradlew connectedDevAndroidTest${NC} # UI tests (needs device/emulator)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

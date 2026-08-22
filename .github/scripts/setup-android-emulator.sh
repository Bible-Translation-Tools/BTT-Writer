#!/usr/bin/env bash
set -euo pipefail

mkdir -p "${HOME}/.android"
cat > "${HOME}/.android/advancedFeatures.ini" <<'EOF'
Vulkan = off
GLDirectMem = on
EOF

echo "Configured ${HOME}/.android/advancedFeatures.ini:"
cat "${HOME}/.android/advancedFeatures.ini"

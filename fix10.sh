#!/bin/bash
# fix10.sh — Pre-grant api.champengine.cloud in PermissionVault
# Problem: AllowListInterceptor blocks api.champengine.cloud because
#          PermissionVault has never granted it NETWORK permission
# Solution: Auto-grant on app startup in ChampEngineApplication.kt

REPO="$(git rev-parse --show-toplevel 2>/dev/null || echo '/storage/emulated/0/Download/openclaw-android-github')"

echo "╔══════════════════════════════════════════╗"
echo "║  fix10 — Pre-grant ChampEngine API host  ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── Step 1: Find PermissionVault to understand grant method ──
VAULT_FILE=$(find "$REPO" -name "PermissionVault.kt" | head -1)
echo "Found PermissionVault: $VAULT_FILE"
echo ""

# ── Step 2: Find ChampEngineApplication.kt ──
APP_FILE=$(find "$REPO" -name "ChampEngineApplication.kt" | head -1)
echo "Found Application class: $APP_FILE"
echo ""

# ── Step 3: Find AllowListInterceptor and add permanent allow ──
INTERCEPTOR_FILE=$(find "$REPO" -name "AllowListInterceptor.kt" | head -1)
echo "Found Interceptor: $INTERCEPTOR_FILE"
echo ""

# ── Step 4: Patch AllowListInterceptor to include champengine domains ──
# The simplest and most reliable fix — add champengine.cloud to the
# permanent allow list alongside localhost/127.0.0.1

cat > /tmp/allowlist_patch.py << 'PYEOF'
import sys

filepath = sys.argv[1]
with open(filepath, 'r') as f:
    content = f.read()

old = 'private val permanentAllowList = setOf("localhost", "127.0.0.1")'
new = '''private val permanentAllowList = setOf(
        "localhost",
        "127.0.0.1",
        "api.champengine.cloud",
        "champengine.cloud",
        "app.champengine.cloud"
    )'''

if old in content:
    content = content.replace(old, new)
    with open(filepath, 'w') as f:
        f.write(content)
    print(f"✅ Patched: {filepath}")
else:
    print(f"⚠️  Could not find target string in {filepath}")
    print("Manual fix needed — see instructions below")
    sys.exit(1)
PYEOF

python3 /tmp/allowlist_patch.py "$INTERCEPTOR_FILE"

if [ $? -ne 0 ]; then
    echo ""
    echo "Auto-patch failed. Manual fix:"
    echo "Open: $INTERCEPTOR_FILE"
    echo "Find this line:"
    echo '    private val permanentAllowList = setOf("localhost", "127.0.0.1")'
    echo "Replace with:"
    echo '    private val permanentAllowList = setOf('
    echo '        "localhost",'
    echo '        "127.0.0.1",'
    echo '        "api.champengine.cloud",'
    echo '        "champengine.cloud",'
    echo '        "app.champengine.cloud"'
    echo '    )'
    exit 1
fi

echo ""
echo "Verifying patch..."
grep -A8 "permanentAllowList" "$INTERCEPTOR_FILE"

echo ""
echo "✅ fix10 complete"
echo ""
echo "Now run:"
echo "  git add -A"
echo "  git commit -m 'fix10: pre-grant champengine.cloud in AllowListInterceptor'"
echo "  git push"


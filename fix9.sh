#!/bin/bash
# fix9.sh — Remove duplicate PNG launcher icons
# Problem: Both .png and .xml ic_launcher files exist in every mipmap folder
# Solution: Delete the old .png files, keep the new .xml vector drawables

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || echo '.')"
RES_DIR="$REPO_ROOT/app/src/main/res"

echo "╔══════════════════════════════════════════╗"
echo "║  fix9 — Remove duplicate launcher PNGs  ║"
echo "╚══════════════════════════════════════════╝"
echo ""

MIPMAP_DIRS=(
    "mipmap-mdpi"
    "mipmap-hdpi"
    "mipmap-xhdpi"
    "mipmap-xxhdpi"
    "mipmap-xxxhdpi"
)

FILES_TO_DELETE=(
    "ic_launcher.png"
    "ic_launcher_round.png"
    "ic_launcher_foreground.png"
    "ic_launcher_background.png"
)

DELETED=0

for dir in "${MIPMAP_DIRS[@]}"; do
    for file in "${FILES_TO_DELETE[@]}"; do
        TARGET="$RES_DIR/$dir/$file"
        if [ -f "$TARGET" ]; then
            rm "$TARGET"
            echo "  ✓ Deleted: $dir/$file"
            DELETED=$((DELETED + 1))
        fi
    done
done

echo ""
echo "Deleted $DELETED duplicate PNG files"
echo ""

# Verify XML files are still present
echo "Verifying XML vector icons remain:"
for dir in "${MIPMAP_DIRS[@]}"; do
    for xml in "ic_launcher.xml" "ic_launcher_round.xml"; do
        TARGET="$RES_DIR/$dir/$xml"
        if [ -f "$TARGET" ]; then
            echo "  ✓ $dir/$xml"
        else
            echo "  ✗ MISSING: $dir/$xml — this needs to exist!"
        fi
    done
done

echo ""
echo "✅ fix9 complete — run your build again"


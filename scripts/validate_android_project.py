from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
res = root / "app/src/main/res"
manifest = root / "app/src/main/AndroidManifest.xml"

errors = []
for f in list(res.rglob("*.xml")) + [manifest]:
    try:
        ET.parse(f)
    except Exception as exc:
        errors.append(f"{f.relative_to(root)}: {exc}")

required = [
    res / "mipmap-anydpi-v26/ic_launcher.xml",
    res / "mipmap-anydpi-v26/ic_launcher_round.xml",
    res / "drawable/ic_launcher_foreground.png",
    res / "values/ic_launcher_background.xml",
]
for f in required:
    if not f.exists():
        errors.append(f"Missing {f.relative_to(root)}")

text = manifest.read_text()
for token in [
    '@mipmap/ic_launcher',
    '@mipmap/ic_launcher_round',
    'android.intent.action.MAIN',
    'android.intent.category.LAUNCHER',
]:
    if token not in text:
        errors.append(f"Manifest missing {token}")

main = root / "app/src/main/java/com/alyshapursley/punctuowlity/MainActivity.java"
if "import androidx.gridlayout.widget.GridLayout;" not in main.read_text():
    errors.append("MainActivity must import androidx.gridlayout.widget.GridLayout")

if errors:
    print("Android project validation FAILED")
    for e in errors:
        print("-", e)
    sys.exit(1)
print("Android project validation PASS")

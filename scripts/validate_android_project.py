from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
app = root / "app"
res = app / "src/main/res"
manifest = app / "src/main/AndroidManifest.xml"
java_root = app / "src/main/java"

errors = []

required_paths = [
    root / "settings.gradle",
    root / "build.gradle",
    root / "gradle.properties",
    root / "gradle/libs.versions.toml",
    app / "build.gradle",
    app / "proguard-rules.pro",
    manifest,
    res / "mipmap-anydpi-v26/ic_launcher.xml",
    res / "mipmap-anydpi-v26/ic_launcher_round.xml",
    res / "drawable/ic_launcher_foreground.png",
    res / "values/ic_launcher_background.xml",
]
for path in required_paths:
    if not path.exists():
        errors.append(f"Missing {path.relative_to(root)}")

for xml_file in list(res.rglob("*.xml")) + [manifest]:
    try:
        ET.parse(xml_file)
    except Exception as exc:
        errors.append(f"{xml_file.relative_to(root)}: {exc}")

settings_text = (root / "settings.gradle").read_text()
if not re.search(r"include\s*[( ]?['\"]:app['\"]", settings_text):
    errors.append("settings.gradle must include the :app module")

manifest_text = manifest.read_text()
for token in [
    "@mipmap/ic_launcher",
    "@mipmap/ic_launcher_round",
    "android.intent.action.MAIN",
    "android.intent.category.LAUNCHER",
]:
    if token not in manifest_text:
        errors.append(f"Manifest missing {token}")

main = java_root / "com/alyshapursley/punctuowlity/MainActivity.java"
if "import androidx.gridlayout.widget.GridLayout;" not in main.read_text():
    errors.append("MainActivity must import androidx.gridlayout.widget.GridLayout")

string_names = set()
strings_file = res / "values/strings.xml"
if strings_file.exists():
    string_names = {
        element.attrib["name"]
        for element in ET.parse(strings_file).getroot().findall("string")
        if "name" in element.attrib
    }

for java_file in java_root.rglob("*.java"):
    text = java_file.read_text()
    for name in re.findall(r"R\.string\.([A-Za-z0-9_]+)", text):
        if name not in string_names:
            errors.append(
                f"{java_file.relative_to(root)} references missing string resource {name}"
            )

invalid_placeholders = [
    path.relative_to(root)
    for path in res.rglob("*")
    if path.is_file() and path.name in {".gitkeep", "git.keep"}
]
for path in invalid_placeholders:
    errors.append(f"Invalid Android resource placeholder: {path}")

if errors:
    print("Android project validation FAILED")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("Android project validation PASS")

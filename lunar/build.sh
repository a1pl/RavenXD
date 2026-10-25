#!/usr/bin/env bash
# Usage: ./build.sh [/path/to/RavenXD-3.1.jar]   (defaults to ../build/libs/RavenXD-3.1.jar)
# Needs JDK 11+ (javac --release 8), python3, unzip, jar.
# Builds dist/RavenXD.jar (the Lunar-patched client) and dist/RavenXDLoader-1.0.meowtils (the Meowtils
# extension, with RavenXD.jar bundled). The loader is compiled against the Meowtils jar: put it in libs/
# or set MEOWTILS_JAR. The verify step uses the vanilla jar and SRG mappings from the Gradle Loom cache.
set -euo pipefail
IN="$(realpath "${1:-$(dirname "$0")/../build/libs/RavenXD-3.1.jar}")"; ROOT="$(cd "$(dirname "$0")" && pwd)"; W="$ROOT/work"
rm -rf "$W"; mkdir -p "$W"/{orig,stubs,tool,patched,shim,mix,out} "$ROOT/dist"
J="javac --release 8 -nowarn"
unzip -q -o "$IN" -d "$W/orig"
$J -d "$W/stubs" $(find "$ROOT/stubs" -name '*.java')
# the bundled Mixin/ASM inside the original jar is used to compile and run the patcher
$J -cp "$W/orig" -d "$W/tool" "$ROOT/tools/Patch.java"
java -cp "$W/orig:$W/tool" Patch "$W/orig" "$W/patched"
$J -cp "$W/stubs:$W/patched" -d "$W/shim" $(find "$ROOT/src/shim" -name '*.java')
$J -cp "$W/stubs:$W/shim:$W/patched" -d "$W/mix" $(find "$ROOT/src/mixins" -name '*.java')
cp -r "$W/patched/." "$W/out/"; cp -r "$W/shim/." "$W/out/"; cp -r "$W/mix/." "$W/out/"
python3 -I - "$W/out" "$ROOT/src/lunar.refmap.part.json" <<'PY'
import json,os,sys
out,part=sys.argv[1],sys.argv[2]
old=json.load(open(out+'/clickgui.mixins.json'))
lunar=sorted("lunar."+f[:-6] for f in os.listdir(out+'/openraven/ravenxd/mixin/lunar') if f.endswith('.class'))
cfg={"required":False,"package":"openraven.ravenxd.mixin","refmap":"mixins.ravenxd.refmap.json","minVersion":"0.6",
 "compatibilityLevel":"JAVA_8","priority":10000,"mixinPriority":10000,"defaultRequire":0,"injectors":{"defaultRequire":0},
 "client":old["client"]+lunar}
json.dump(cfg,open(out+'/mixins.ravenxd.json','w'),indent=2)
rm=json.load(open(out+'/clickgui.refmap.json'))
for k,v in json.load(open(part)).items():
    rm['mappings'][k]=v
    for ctx in rm.get('data',{}).values(): ctx[k]=v
json.dump(rm,open(out+'/mixins.ravenxd.refmap.json','w'),indent=1)
os.remove(out+'/clickgui.refmap.json'); os.remove(out+'/clickgui.mixins.json')
json.dump({"version":"3.1"},open(out+'/version.json','w'))
PY
printf 'Manifest-Version: 1.0\nTweakClass: org.spongepowered.asm.launch.MixinTweaker\nTweakOrder: 0\nMixinConfigs: mixins.ravenxd.json\nFMLCorePluginContainsFMLMod: true\nForceLoadAsMod: true\n\n' > "$W/out/META-INF/MANIFEST.MF"
rm -f "$ROOT/dist/RavenXD.jar"
( cd "$W/out" && jar cfm "$ROOT/dist/RavenXD.jar" META-INF/MANIFEST.MF $(ls | grep -v META-INF) META-INF/services )
echo "built $ROOT/dist/RavenXD.jar"

# Fail the build if anything still needs Forge or a Forge-only member (it would only break in-game).
LOOM="$HOME/.gradle/caches/essential-loom/1.8.9"
if [ -f "$LOOM/minecraft-client.jar" ] && [ -f "$LOOM/srg/1.8.9/srg.tsrg" ]; then
  $J -cp "$W/orig" -d "$W/tool" "$ROOT/tools/Verify.java"
  java -cp "$W/orig:$W/tool" Verify "$W/out" "$LOOM/minecraft-client.jar" "$LOOM/srg/1.8.9/srg.tsrg" \
    "$(ls "$LOOM"/de.oceanlabs.mcp.*/mappings-srg-named.srg | head -1)"
else
  echo "warning: Loom cache not found (build RavenXD with Gradle first), skipping verify"
fi

MEOWTILS_JAR="${MEOWTILS_JAR:-$(ls "$ROOT"/libs/Meowtils-*.jar 2>/dev/null | head -1)}"
if [ -z "$MEOWTILS_JAR" ] || [ ! -f "$MEOWTILS_JAR" ]; then
  echo "warning: Meowtils jar not found (libs/Meowtils-*.jar or MEOWTILS_JAR), skipping the loader extension"; exit 0
fi
mkdir -p "$W/loader/META-INF" "$W/loader/meowtils/ravenxd"
$J -proc:none -cp "$MEOWTILS_JAR" -d "$W/loader" $(find "$ROOT/loader/src" -name '*.java')
printf 'main=meowtils.ravenxd.Main\n' > "$W/loader/META-INF/meowtils.extension"
cp "$ROOT/dist/RavenXD.jar" "$W/loader/meowtils/ravenxd/RavenXD.jar"
rm -f "$ROOT/dist/RavenXDLoader-1.0.meowtils"
( cd "$W/loader" && jar cf "$ROOT/dist/RavenXDLoader-1.0.meowtils" META-INF/meowtils.extension meowtils )
echo "built $ROOT/dist/RavenXDLoader-1.0.meowtils"

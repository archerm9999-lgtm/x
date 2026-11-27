Shield Swap (Fabric, client-side, Minecraft 26.2)

BUILD OPTION A - nothing installed locally (GitHub Actions)
 1. Make a free GitHub repo, upload the CONTENTS of this folder (build.gradle at the repo root,
    plus src/ and .github/). If .github didn't upload, create the file
    .github/workflows/build.yml in the GitHub web UI and paste in the one from this folder.
 2. Open the repo's Actions tab, let the "build" run finish (a few minutes).
 3. Open the run -> Artifacts -> download shield-swap-jar -> unzip it for the .jar.

BUILD OPTION B - on your own PC (JDK 25 + Gradle 9.5.1)
 Copy gradlew, gradlew.bat and the gradle/ folder from the template at
 https://fabricmc.net/develop/template/ (Minecraft 26.2) into this folder, then run
 `gradlew build` (Windows) or `./gradlew build` (Mac/Linux). Jar: build/libs/shield-swap-1.0.0.jar

INSTALL: put the jar in .minecraft/mods with Fabric Loader 0.19.3+ and Fabric API for 26.2.
SETTINGS: constants at the top of ShieldSwapClient.java.

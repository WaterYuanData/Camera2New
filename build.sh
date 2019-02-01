#! bin/bash
echo '555'

./gradlew clean
#./gradlew assembleRelease
#./gradlew assembleDebug
#./gradlew assembleMy
./gradlew installBeta
#./gradlew installRelease
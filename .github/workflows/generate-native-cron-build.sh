#! /bin/sh

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <graalvm version> <java version> <cron>"
    exit 1
fi

GRAALVM_VERSION=$1
JAVA_VERSION=$2
SCHEDULE=$3

rm -f generated-native-cron-build-*.yml
export BUILD_FILE=generated-native-cron-build-graalvm-${GRAALVM_VERSION}-jdk-${JAVA_VERSION}.yml
export TMP_FILE=.generate-native-cron-build.tmp

yq w -i $BUILD_FILE name "Quarkus CI - Nightly Native - GraalVM ${GRAALVM_VERSION} - JDK ${JAVA_VERSION}"
# to test
yq w -i $BUILD_FILE on.push ""
yq w -i $BUILD_FILE on.schedule[+].cron "${SCHEDULE}"

# copy env
yq r ci-actions.yml env > ${TMP_FILE}
yq p -i ${TMP_FILE} env
yq m -i $BUILD_FILE ${TMP_FILE}
# copy jobs.build-jdk11
yq r ci-actions.yml jobs.build-jdk11 > ${TMP_FILE}
yq p -i ${TMP_FILE} jobs.build-jdk11
yq m -i $BUILD_FILE ${TMP_FILE}
# copy jobs.native-tests
yq r ci-actions.yml jobs.native-tests > ${TMP_FILE}
yq p -i ${TMP_FILE} jobs.native-tests
yq m -i $BUILD_FILE ${TMP_FILE}

yq w -i $BUILD_FILE env.GRAALVM_IMAGE_VERSION "${GRAALVM_VERSION}-java${JAVA_VERSION}"
yq d -i $BUILD_FILE jobs.build-jdk11.if
yq w -i $BUILD_FILE 'jobs.build-jdk11.steps(name==Set up JDK).with.java-version' ${JAVA_VERSION}
yq w -i $BUILD_FILE 'jobs.native-tests.steps(name==Set up JDK).with.java-version' ${JAVA_VERSION}

rm ${TMP_FILE}

cat ${BUILD_FILE}

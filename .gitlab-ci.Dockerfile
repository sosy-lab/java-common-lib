# This is a Docker image for running the tests.
# It should be pushed to registry.gitlab.com/sosy-lab/software/java-common-lib/test
# and will be used by CI as declared in .gitlab-ci.yml.
#
# Commands for updating the image:
# docker build -t registry.gitlab.com/sosy-lab/software/java-common-lib/test - < .gitlab-ci.Dockerfile
# docker push registry.gitlab.com/sosy-lab/software/java-common-lib/test

FROM openjdk:8-jdk-slim
RUN apt-get update && apt-get install -y \
  ant \
  curl \
  git \
  jq \
  wget

#!/bin/bash

# This file is part of SoSy-Lab Java-Project Template,
# a collection of common files and build definitions for Java projects:
# https://gitlab.com/sosy-lab/software/java-project-template
#
# SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

set -e # exit with nonzero exit code if anything fails

# DO NOT EDIT LOCALLY!
# Keep this file synchronized with
# https://gitlab.com/sosy-lab/software/java-project-template

rm -rf gh-pages || true
mkdir -p gh-pages/api
cp -r website/* gh-pages
cp -r Javadoc/* gh-pages/api
find bin -name ConfigurationOptions.txt -exec cp {} gh-pages/ \;

# we need to tell git who we are
git config --global user.name "${GIT_NAME}"
git config --global user.email "${GIT_EMAIL}"

cd gh-pages
git init

# The first and only commit to this new Git repo contains all the
# files present with the commit message "Deploy to GitHub Pages".
git add .
git commit -m "Deploy to GitHub Pages"

# Force push from the current repo's HEAD to the remote
# repo's gh-pages branch. (All previous history on the gh-pages branch
# will be lost, since we are overwriting it.) We redirect any output to
# /dev/null to hide any sensitive credential data that might otherwise be exposed.
git push --force --quiet "https://${GH_TOKEN}@${GH_REF}" HEAD:gh-pages > /dev/null 2>&1

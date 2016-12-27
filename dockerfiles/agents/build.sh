#!/bin/sh
# Copyright (c) 2016 Codenvy, S.A.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

IMAGE_NAME="codenvy/agents"
DIR=$(cd "$(dirname "$0")"; pwd)
. ${DIR}/../build.include

mvn clean install -f ${DIR}/pom.xml -U

# build
init "$@"
build "$@"

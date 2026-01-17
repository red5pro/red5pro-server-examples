#!/usr/bin/env bash
set -euo pipefail

mvn clean package -Prelease

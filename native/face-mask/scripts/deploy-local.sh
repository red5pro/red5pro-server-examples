#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${RED5_ROOT:-}" ]]; then
  echo "RED5_ROOT is not set. Example: export RED5_ROOT=/usr/local/red5pro" >&2
  exit 1
fi

jar_path="${1:-target/opencv-facemask.jar}"
plugin_dir="${RED5_ROOT}/plugins"
native_dir="${RED5_ROOT}/plugins/native/facemask"

cp "${jar_path}" "${plugin_dir}/"
mkdir -p "${native_dir}"
cp src/main/resources/module-facemask.xml "${native_dir}/"
cp src/main/resources/haarcascade_frontalface_alt.xml "${native_dir}/"

echo "Deployed to ${plugin_dir} and ${native_dir}"

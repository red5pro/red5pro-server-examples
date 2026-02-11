#!/bin/bash
#
# Build OpenCV libraries and facemask module for CentOS 7.9 compatibility using Docker
#
# Output:
#   - OpenCV libraries -> src/main/lib/amd64-Linux-gpp/
#   - OpenCV headers -> src/main/include/amd64-Linux-gpp/opencv2/
#   - facemask.so -> target/
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LIB_DIR="${PROJECT_ROOT}/src/main/lib/amd64-Linux-gpp"
INCLUDE_DIR="${PROJECT_ROOT}/src/main/include/amd64-Linux-gpp"
TARGET_DIR="${PROJECT_ROOT}/target"

IMAGE_NAME="opencv-centos7-builder"
CONTAINER_NAME="opencv-build-temp"

echo "=== Building OpenCV and Facemask for CentOS 7.9 compatibility ==="
echo "Project root: ${PROJECT_ROOT}"
echo "Library output: ${LIB_DIR}"
echo "Include output: ${INCLUDE_DIR}"
echo "Binary output: ${TARGET_DIR}"
echo ""

# Build the Docker image from project root (for COPY context)
echo "=== Building Docker image (this may take 10-20 minutes on first run) ==="
docker build -t "${IMAGE_NAME}" -f "${SCRIPT_DIR}/Dockerfile" "${PROJECT_ROOT}"

# Remove any existing temp container
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

# Create a container from the image
echo ""
echo "=== Extracting build artifacts ==="
docker create --name "${CONTAINER_NAME}" "${IMAGE_NAME}"

# Ensure output directories exist
mkdir -p "${LIB_DIR}"
mkdir -p "${INCLUDE_DIR}"
mkdir -p "${TARGET_DIR}"

# Copy OpenCV libraries
docker cp "${CONTAINER_NAME}:/output/lib/libopencv_core.so" "${LIB_DIR}/"
docker cp "${CONTAINER_NAME}:/output/lib/libopencv_imgproc.so" "${LIB_DIR}/"
docker cp "${CONTAINER_NAME}:/output/lib/libopencv_objdetect.so" "${LIB_DIR}/"
docker cp "${CONTAINER_NAME}:/output/lib/libopencv_imgcodecs.so" "${LIB_DIR}/"

# Copy headers (replace existing)
rm -rf "${INCLUDE_DIR}/opencv2"
docker cp "${CONTAINER_NAME}:/output/include/opencv2" "${INCLUDE_DIR}/"

# Copy facemask binary to target
docker cp "${CONTAINER_NAME}:/output/target/facemask.so" "${TARGET_DIR}/"

# Cleanup
docker rm -f "${CONTAINER_NAME}"

echo ""
echo "=== Build complete ==="
echo ""
echo "OpenCV libraries installed to ${LIB_DIR}:"
ls -la "${LIB_DIR}"/libopencv*.so
echo ""
echo "OpenCV headers installed to ${INCLUDE_DIR}/opencv2/"
echo ""
echo "Facemask binary installed to ${TARGET_DIR}:"
ls -la "${TARGET_DIR}"/facemask.so
echo ""
echo "All binaries are CentOS 7.9 compatible (glibc 2.17+)"

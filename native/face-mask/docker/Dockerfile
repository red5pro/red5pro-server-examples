# CentOS 7.9 base for maximum glibc compatibility (glibc 2.17)
FROM centos:7.9.2009

# Fix CentOS 7 EOL mirror issues - switch to vault for all repos
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*.repo && \
    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*.repo

# Install EPEL repo and fix its mirror to use archive
RUN yum install -y epel-release && \
    sed -i 's|metalink=|#metalink=|g' /etc/yum.repos.d/epel*.repo && \
    sed -i 's|#baseurl=http://download.fedoraproject.org/pub/epel|baseurl=https://archives.fedoraproject.org/pub/archive/epel|g' /etc/yum.repos.d/epel*.repo && \
    yum clean all

# Install SCL repo and fix ALL scl-related repos to use vault
RUN yum install -y centos-release-scl && \
    sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-SCL*.repo && \
    sed -i 's|# baseurl=|baseurl=|g' /etc/yum.repos.d/CentOS-SCL*.repo && \
    sed -i 's|#baseurl=|baseurl=|g' /etc/yum.repos.d/CentOS-SCL*.repo && \
    sed -i 's|baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-SCL*.repo && \
    yum clean all

# Install build dependencies including Java for JNI headers
RUN yum install -y \
    devtoolset-11-gcc \
    devtoolset-11-gcc-c++ \
    devtoolset-11-make \
    cmake3 \
    git \
    wget \
    pkg-config \
    gtk3-devel \
    libjpeg-turbo-devel \
    libpng-devel \
    libtiff-devel \
    zlib-devel \
    java-11-openjdk-devel \
    && yum clean all

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk

# Set OpenCV version
ARG OPENCV_VERSION=4.13.0

# Enable devtoolset-11 for all subsequent commands
SHELL ["/usr/bin/scl", "enable", "devtoolset-11", "--", "/bin/bash", "-c"]

# Clone OpenCV source
WORKDIR /build
RUN git clone --branch ${OPENCV_VERSION} --depth 1 https://github.com/opencv/opencv.git

# Build OpenCV with only required modules
WORKDIR /build/opencv/build
RUN cmake3 .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX=/opt/opencv \
    -DBUILD_SHARED_LIBS=ON \
    -DBUILD_LIST=core,imgproc,objdetect,imgcodecs \
    -DBUILD_opencv_apps=OFF \
    -DBUILD_opencv_python2=OFF \
    -DBUILD_opencv_python3=OFF \
    -DBUILD_TESTS=OFF \
    -DBUILD_PERF_TESTS=OFF \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_DOCS=OFF \
    -DWITH_FFMPEG=OFF \
    -DWITH_GSTREAMER=OFF \
    -DWITH_V4L=OFF \
    -DWITH_QT=OFF \
    -DWITH_GTK=OFF \
    -DWITH_OPENGL=OFF \
    -DWITH_CUDA=OFF \
    -DWITH_OPENCL=OFF \
    -DWITH_IPP=OFF \
    -DWITH_TBB=OFF \
    -DWITH_EIGEN=OFF \
    -DWITH_LAPACK=OFF \
    -DCMAKE_CXX_FLAGS="-fPIC" \
    -DCMAKE_C_FLAGS="-fPIC"

RUN make -j$(nproc) && make install

# Create output directory structure
RUN mkdir -p /output/lib /output/include /output/target

# Copy OpenCV libraries
RUN cp /opt/opencv/lib64/libopencv_core.so.${OPENCV_VERSION} /output/lib/libopencv_core.so && \
    cp /opt/opencv/lib64/libopencv_imgproc.so.${OPENCV_VERSION} /output/lib/libopencv_imgproc.so && \
    cp /opt/opencv/lib64/libopencv_objdetect.so.${OPENCV_VERSION} /output/lib/libopencv_objdetect.so && \
    cp /opt/opencv/lib64/libopencv_imgcodecs.so.${OPENCV_VERSION} /output/lib/libopencv_imgcodecs.so && \
    cp -r /opt/opencv/include/opencv4/opencv2 /output/include/

# Copy facemask source for building
WORKDIR /build/facemask
COPY src/main/c++/*.cpp src/main/c++/*.h ./
COPY src/main/c++/makefile ./

# Build facemask module using the freshly built OpenCV
RUN g++ -fpermissive \
    -I${JAVA_HOME}/include \
    -I${JAVA_HOME}/include/linux \
    -I/opt/opencv/include/opencv4 \
    -fPIC -shared -g \
    -c -o cauldron_test.o cauldron_test.cpp

RUN g++ -fPIC -shared -g \
    -I${JAVA_HOME}/include \
    -I${JAVA_HOME}/include/linux \
    -I/opt/opencv/include/opencv4 \
    cauldron_test.o -o /output/target/facemask.so \
    -L/opt/opencv/lib64 \
    -lopencv_core -lopencv_imgproc -lopencv_objdetect \
    -Wl,-rpath,'$ORIGIN'

# Verify outputs
RUN echo "=== OpenCV Libraries ===" && ls -la /output/lib/ && \
    echo "=== Facemask Binary ===" && ls -la /output/target/ && \
    echo "=== Library Dependencies ===" && ldd /output/target/facemask.so | head -20

CMD ["echo", "Build complete. Use docker cp to extract from /output/"]

FROM openjdk:11-jdk-slim

# Установка переменных окружения
ENV GRADLE_PROFILER_VERSION="0.20.0"
ENV CMDLINE_TOOLS_VERSION="8512546_latest"
ENV SDK_ROOT="/sdk"
ENV GRADLE_PROFILER_DIR="/gradle-profiler"
ENV CMDLINE_TOOLS_DIR="${SDK_ROOT}/cmdline-tools"
ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=en_US.UTF-8

# Установка зависимостей и настройка локалей
RUN dpkg --add-architecture i386 && apt-get update -yqq && \
    apt-get install -y \
    curl \
    expect \
    git \
    make \
    git-core \
    libc6:i386 \
    libgcc1:i386 \
    libncurses5:i386 \
    libstdc++6:i386 \
    zlib1g:i386 \
    openjdk-11-jdk \
    wget \
    unzip \
    vim \
    openssh-client \
    locales && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    localedef -i en_US -c -f UTF-8 en_US.UTF-8

# Создание пользователя и групп
RUN groupadd -g 1000 jenkins && \
    groupadd -g 999 docker && \
    groupadd -g 994 dockerroot && \
    useradd --create-home -d "/home/jenkins" -u 1000 -g jenkins -G docker,dockerroot jenkins

# Настройка директории SDK и изменение прав доступа
RUN mkdir -p ${SDK_ROOT} && chown -R jenkins:jenkins ${SDK_ROOT}

# Загрузка и установка Gradle Profiler
RUN mkdir -p ${GRADLE_PROFILER_DIR} && \
    cd ${GRADLE_PROFILER_DIR} && \
    wget -O gradle-profiler.zip https://repo1.maven.org/maven2/org/gradle/profiler/gradle-profiler/${GRADLE_PROFILER_VERSION}/gradle-profiler-${GRADLE_PROFILER_VERSION}.zip && \
    unzip gradle-profiler.zip && \
    rm gradle-profiler.zip

# Загрузка и установка Android SDK Command Line Tools
RUN cd ${SDK_ROOT} && \
    wget -O cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}.zip && \
    unzip cmdline-tools.zip -d ${SDK_ROOT} && \
    rm cmdline-tools.zip

# Принятие лицензий SDK
RUN yes | ${CMDLINE_TOOLS_DIR}/bin/sdkmanager --licenses --sdk_root=SDK_ROOT

# Обновление переменных окружения в .bashrc
RUN echo "export ANDROID_SDK_ROOT=${SDK_ROOT}" >> /etc/profile.d/android.sh && \
    echo "export PATH=${SDK_ROOT}/cmdline-tools/latest/bin:${SDK_ROOT}/platform-tools:\$PATH" >> /etc/profile.d/android.sh

# Переключение на пользователя jenkins
USER jenkins
WORKDIR /home/jenkins

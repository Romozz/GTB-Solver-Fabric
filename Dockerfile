# Используем образ openjdk 21 для Minecraft
FROM openjdk:21-jdk-slim

# Устанавливаем зависимости для Minecraft и добавляем утилиту ping
RUN apt-get update && apt-get install -y curl unzip iputils-ping && \
    echo "Dependencies installed successfully" 

# Создаем директорию /minecraft, если её нет
RUN mkdir -p /minecraft

# Создаем директорию .minecraft и корректные файлы конфигурации
RUN mkdir -p /minecraft/.minecraft/versions/1.21.4 && \
    echo '{}' > /minecraft/.minecraft/launcher_profiles.json && \
    echo "Empty launcher profile created"

# Устанавливаем Fabric для Minecraft 1.21.4
RUN echo "Downloading Fabric installer" && \
    curl -Lo fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.0/fabric-installer-1.1.0.jar && \
    echo "Fabric installer downloaded successfully" && \
    # Создаем директорию и инициализируем Minecraft
    java -jar fabric-installer.jar client -mcversion 1.21.4 -loader 0.16.9 -dir /minecraft || \
    { echo "Fabric installation failed"; exit 1; }

# Принимаем путь к модификации как аргумент
ARG MOD_PATH
# Копируем мод из артефакта в папку mods
COPY ${MOD_PATH} /minecraft/mods/

# Устанавливаем EULA
RUN echo "eula=true" > /minecraft/eula.txt

# Устанавливаем рабочую директорию
WORKDIR /minecraft

# Запускаем Minecraft клиент с Fabric
CMD ["java", "-Xmx2G", "-jar", "fabric-loader-0.16.9.jar", "nogui"]

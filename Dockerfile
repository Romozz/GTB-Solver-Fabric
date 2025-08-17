# Используем образ openjdk для Minecraft
FROM openjdk:17-jdk-slim

# Устанавливаем зависимости для Minecraft
RUN apt-get update && apt-get install -y curl unzip

# Устанавливаем Fabric для Minecraft 1.21.4
RUN curl -Lo fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.0/fabric-installer-1.1.0.jar \
    && java -jar fabric-installer.jar client -mcversion 1.21.4 -loader 0.16.9 -dir /minecraft

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

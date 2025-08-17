# Используем официальный образ Minecraft Java Server как базу
FROM itzg/minecraft-java-server:latest

# Устанавливаем необходимые зависимости
RUN apt-get update && apt-get install -y curl unzip

# Устанавливаем Fabric для версии 1.21.4
RUN curl -Lo fabric-installer.jar https://maven.fabricmc.net/net/fabricmc/fabric-installer/0.11.3/fabric-installer-0.11.3.jar \
    && java -jar fabric-installer.jar client -mcversion 1.21.4 -loader 0.16.9 -dir /minecraft

# Копируем ваш мод в контейнер
COPY ./build/libs/modid-1.0.0.jar /minecraft/mods/

# Устанавливаем EULA
RUN echo "eula=true" > /minecraft/eula.txt

# Устанавливаем рабочую директорию
WORKDIR /minecraft

# Запускаем Minecraft клиент
CMD ["java", "-Xmx2G", "-jar", "fabric-loader-0.16.9.jar", "nogui"]

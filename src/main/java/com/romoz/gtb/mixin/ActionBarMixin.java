package com.romoz.gtb.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ActionBarMixin {
    // Внедряемся в метод, который отвечает за обработку сообщений о игроках
    @Inject(at = @At("HEAD"), method = "onPlayerList(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket;)V")
    private void onPlayerJoin(PlayerListS2CPacket packet, CallbackInfo info) {
        // Когда в список игроков поступает новый пакет, выводим сообщение
        System.out.println("[GTBSolver] A player has joined or left the game.");
    }
}

package io.github.qan4ur.vetmos.mixin;

import io.github.qan4ur.vetmos.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    /** Runs on the main thread (after forceMainThread), before vanilla handles the status. */
    @Inject(method = "onEntityStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void vetmos$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        Entity entity = packet.getEntity(mc.world);
        if (entity != null) ModuleManager.entityStatus(entity, packet.getStatus());
    }
}

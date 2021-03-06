package io.github.axolotlclient.mixin;

import io.github.axolotlclient.AxolotlClient;
import io.github.axolotlclient.NetworkHelper;
import io.github.axolotlclient.modules.hud.HudManager;
import io.github.axolotlclient.modules.hud.gui.hud.CPSHud;
import io.github.axolotlclient.modules.sky.SkyResourceManager;
import io.github.axolotlclient.modules.zoom.Zoom;
import io.github.axolotlclient.util.DiscordRPC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.world.level.LevelInfo;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Final private String gameVersion;

    @Shadow public GameOptions options;

    @Shadow public ClientPlayerEntity player;

    @Shadow protected abstract void loadLogo(TextureManager textureManager) throws LWJGLException;

    @Shadow private TextureManager textureManager;

    /**
     * @author TheKodeToad & Sk1erLLC (initially created this fix).
     * @reason unnecessary garbage collection
     */
    @Redirect(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
    public void noWorldGC() {
    }

    /**
     * @author moehreag
     * @reason Customize Window title for use in AxolotlClient
     */
    @Inject(method = "setPixelFormat", at = @At("TAIL"))
    public void setWindowTitle(CallbackInfo ci){
        Display.setTitle("AxolotlClient "+ this.gameVersion);
    }

    @Redirect(method = "initializeGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;loadLogo(Lnet/minecraft/client/texture/TextureManager;)V"))
    public void noLogo(MinecraftClient instance, TextureManager textureManager){}

    @Inject(method = "initializeGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/Achievement;setStatFormatter(Lnet/minecraft/stat/StatFormatter;)Lnet/minecraft/advancement/Achievement;"))
    public void loadSkiesOnStartup(CallbackInfo ci){
        SkyResourceManager.onStartup();
        try {
            this.loadLogo(this.textureManager);
        } catch (Exception ignored){}
    }

    @Redirect(method = "handleKeyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;getCode()I", ordinal = 5))
    // Fix taking a screenshot when pressing '<' (Because it has the same keyCode as F2)
    public int iTryToFixTheScreenshotKey(KeyBinding instance) {

        if (Keyboard.getEventCharacter() != '<') {
            return instance.getCode();
        }

        return -999; // There is no key with this Code, but this is what we want here.
    }

    // Don't ask me why we need both here, but otherwise it looks ugly
    @Redirect(method = "loadLogo", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;color(IIII)Lnet/minecraft/client/render/BufferBuilder;"))
    public BufferBuilder loadingScreenColor(BufferBuilder instance, int red, int green, int blue, int alpha){

        return instance.color(AxolotlClient.CONFIG.loadingScreenColor.get().getRed(),
                AxolotlClient.CONFIG.loadingScreenColor.get().getGreen(),
                AxolotlClient.CONFIG.loadingScreenColor.get().getBlue(), AxolotlClient.CONFIG.loadingScreenColor.get().getAlpha());
    }

    @Redirect(method = "method_9382", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;color(IIII)Lnet/minecraft/client/render/BufferBuilder;"))
    public BufferBuilder loadingScreenBg(BufferBuilder instance, int red, int green, int blue, int alpha){

        return instance.color(AxolotlClient.CONFIG.loadingScreenColor.get().getRed(),
                AxolotlClient.CONFIG.loadingScreenColor.get().getGreen(),
                AxolotlClient.CONFIG.loadingScreenColor.get().getBlue(), AxolotlClient.CONFIG.loadingScreenColor.get().getAlpha());
    }

    @Redirect(
            method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/RunArgs$Game;version:Ljava/lang/String;"))
    private String redirectVersion(RunArgs.Game game) {
        return "1.8.9";
    }

    @Inject(method = "startGame", at = @At("HEAD"))
    public void startup(String worldName, String string, LevelInfo levelInfo, CallbackInfo ci){
        DiscordRPC.startup();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo ci){
        NetworkHelper.setOffline();
        DiscordRPC.shutdown();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickClient(CallbackInfo ci){
        AxolotlClient.tickClient();
    }


    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"), remap = false)
    public int onScroll() {
        int amount = Mouse.getEventDWheel();
        if(amount != 0) {
            if(Zoom.scroll(amount)) {
                return 0;
            }
        }
        return amount;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getTime()J", ordinal = 0))
    public void onMouseButton(CallbackInfo ci){
        CPSHud cpshud = (CPSHud) HudManager.getINSTANCE().get(CPSHud.ID);
        if(cpshud.isEnabled()){
            cpshud.click();
        }
    }

    @Inject(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;method_1236(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    public void login(ClientWorld world, String loadingMessage, CallbackInfo ci){
        NetworkHelper.setOnline();
    }

}

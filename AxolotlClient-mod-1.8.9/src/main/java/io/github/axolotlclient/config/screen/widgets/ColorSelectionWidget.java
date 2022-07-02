package io.github.axolotlclient.config.screen.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.axolotlclient.config.Color;
import io.github.axolotlclient.config.options.ColorOption;
import io.github.axolotlclient.modules.hud.util.DrawUtil;
import io.github.axolotlclient.modules.hud.util.Rectangle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ColorSelectionWidget extends ButtonWidget {
    private final ColorOption option;

    protected Rectangle pickerImage;
    //private final Rectangle rect;

    public ColorSelectionWidget(ColorOption option) {
        super(0, 100, 50, "");
        this.option=option;
        Window window= new Window(MinecraftClient.getInstance());
        width=window.getWidth()-280;
        height=window.getHeight()-200;

        pickerImage = new Rectangle(170, 150, width/4, height/2);

        //rect = new Rectangle(100, 50, width-200, height-100);
    }

    @Override
    public void render(MinecraftClient client, int mouseX, int mouseY) {

        DrawUtil.fillRect(new Rectangle(145, 65, width, height), Color.DARK_GRAY.withAlpha(127));
        DrawUtil.outlineRect(new Rectangle(145, 65, width, height), Color.BLACK);

        DrawUtil.outlineRect(pickerImage, Color.DARK_GRAY.withAlpha(127));

        GlStateManager.color3f(1, 1, 1);
        MinecraftClient.getInstance().getTextureManager().bindTexture(new Identifier("axolotlclient", "textures/gui/colorwheel.png"));
        DrawableHelper.drawTexture(pickerImage.x, pickerImage.y, 0, 0, pickerImage.width, pickerImage.height, pickerImage.width, pickerImage.height);

        //super.render(client, mouseX, mouseY);
    }

    public void onClick(int mouseX, int mouseY){
        final ObjectColorPicker picker = pickerInfo.getPicker();
        OnObjectPickedListener listener = picker.mObjectPickedListener;
        if (listener != null) {
            final ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4);
            pixelBuffer.order(ByteOrder.nativeOrder());
            GL11.glReadPixels(pickerInfo.getX(),
                    picker.mRenderer.getViewportHeight() - pickerInfo.getY(),
                    1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
            GL11.glBindFramebuffer(GL11.GL_FRAMEBUFFER, 0);
            pixelBuffer.rewind();
            final int r = pixelBuffer.get(0) & 0xff;
            final int g = pixelBuffer.get(1) & 0xff;
            final int b = pixelBuffer.get(2) & 0xff;
            final int a = pixelBuffer.get(3) & 0xff;
            final int index = Color.argb(a, r, g, b);
            if (0 <= index && index < picker.mObjectLookup.size()) {
                // Index may have holes due to unregistered objects
                Object3D pickedObject = picker.mObjectLookup.get(index);
                if (pickedObject != null) {
                    listener.onObjectPicked(pickedObject);
                    return;
                }
            }
            listener.onNoObjectPicked();
        }
        }
    }

    private static class GL_FRAMEBUFFER {
    }
}

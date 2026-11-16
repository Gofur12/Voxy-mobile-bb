package me.cortex.voxy.client;

import me.cortex.voxy.common.Logger;
import me.cortex.voxy.commonImpl.ImportManager;
import me.cortex.voxy.commonImpl.importers.IDataImporter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;

import java.util.UUID;

//1.20.1 version. Differences to the main (26.x) source set:
// - Gui#hud does not exist, the boss overlay is Gui#getBossOverlay() and chat is Gui#getChat()
// - BossHealthOverlay#events is not accessible, so the bar is driven through the public
//   BossHealthOverlay#update(ClientboundBossEventPacket) API instead of poking the map directly
public class ClientImportManager extends ImportManager {
    protected class ClientImportTask extends ImportTask {
        private final UUID bossbarUUID;
        private final BossEvent bossBar;
        protected ClientImportTask(IDataImporter importer) {
            super(importer);

            this.bossbarUUID = UUID.randomUUID();
            this.bossBar = new BossEvent(this.bossbarUUID, Component.nullToEmpty("Voxy world importer"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS) {};
            Minecraft.getInstance().execute(()->{
                Minecraft.getInstance().gui.getBossOverlay().update(ClientboundBossEventPacket.createAddPacket(this.bossBar));
            });
        }

        @Override
        protected boolean onUpdate(int completed, int outOf) {
            if (!super.onUpdate(completed, outOf)) {
                return false;
            }
            Minecraft.getInstance().execute(()->{
                this.bossBar.setProgress((float) (((double)completed) / ((double) Math.max(1, outOf))));
                this.bossBar.setName(Component.nullToEmpty("Voxy import: " + completed + "/" + outOf + " chunks"));
                var overlay = Minecraft.getInstance().gui.getBossOverlay();
                overlay.update(ClientboundBossEventPacket.createUpdateProgressPacket(this.bossBar));
                overlay.update(ClientboundBossEventPacket.createUpdateNamePacket(this.bossBar));
            });
            return true;
        }

        @Override
        protected void onCompleted(int total) {
            super.onCompleted(total);
            Minecraft.getInstance().execute(()->{
                Minecraft.getInstance().gui.getBossOverlay().update(ClientboundBossEventPacket.createRemovePacket(this.bossbarUUID));
                long delta = Math.max(System.currentTimeMillis() - this.startTime, 1);

                String msg = "Voxy world import finished in " + (delta/1000) + " seconds, averaging " + (int)(total/(delta/1000f)) + " chunks per second";
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal(msg));
                Logger.info(msg);
            });
        }
    }

    @Override
    protected synchronized ImportTask createImportTask(IDataImporter importer) {
        return new ClientImportTask(importer);
    }
}

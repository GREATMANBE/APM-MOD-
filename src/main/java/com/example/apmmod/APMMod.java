package com.example.apmmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = "apmmod", name = "Simple APM Hotbar Tracker", version = "1.0", acceptedMinecraftVersions = "[1.8.9]")
public class APMMod {

    private int previousSlot = -1;
    private long lastSwitchTime = -1;

    private final APMTracker apmTracker = new APMTracker();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Detect hotbar slot switching on client tick
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        int currentSlot = mc.thePlayer.inventory.currentItem; // current hotbar slot (0-based)

        // We're only interested in slots 2, 3, 4 (0-based, so 1, 2, 3)
        // but your original code used 2-4 1-based. Let's adapt accordingly:
        // Minecraft hotbar slots are 0-8, so slots 2,3,4 means indexes 1,2,3?

        if (currentSlot < 1 || currentSlot > 3) {
            previousSlot = -1; // reset if outside range
            return;
        }

        if (previousSlot != currentSlot) {
            long currentTime = System.currentTimeMillis();
            if (previousSlot != -1 && lastSwitchTime != -1) {
                long interval = currentTime - lastSwitchTime;
                System.out.println("Switched from slot " + previousSlot + " to " + currentSlot +
                        " | Interval: " + interval + " ms");
            }
            previousSlot = currentSlot;
            lastSwitchTime = currentTime;

            apmTracker.recordSwitch(currentTime, currentSlot);
        }
    }

    // Render simple APM and slot switch times on screen
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();

        int x = 5;
        int y = 5;
        int color = 0xFFFFFF;

        String apmText = "APM: " + apmTracker.getActionsPerMinute();
        mc.fontRendererObj.drawString(apmText, x, y, color);
        y += 12;

        for (int slot = 2; slot <= 4; slot++) {
            int avg = apmTracker.getAverageStayTime(slot);
            String slotText = String.format("Slot %d avg stay: %d ms", slot, avg);
            mc.fontRendererObj.drawString(slotText, x, y, color);
            y += 12;
        }
    }

    // Tracks hotbar switches and timings
    private static class APMTracker {

        private final Queue<Long> actionTimes = new LinkedList<>();
        private final Map<Integer, List<Long>> slotStayTimes = new HashMap<>();

        private int lastSlot = -1;
        private long lastSwitchTime = 0;

        public void recordSwitch(long time, int slot) {
            actionTimes.add(time);

            // Remove timestamps older than 60 seconds for APM calculation
            long cutoff = time - 60000L;
            while (!actionTimes.isEmpty() && actionTimes.peek() < cutoff) {
                actionTimes.poll();
            }

            if (lastSlot != -1 && lastSlot != slot) {
                long duration = time - lastSwitchTime;
                slotStayTimes.putIfAbsent(lastSlot, new ArrayList<>());
                List<Long> durations = slotStayTimes.get(lastSlot);
                durations.add(duration);
                if (durations.size() > 10) {
                    durations.remove(0);
                }
            }

            lastSlot = slot;
            lastSwitchTime = time;
        }

        public int getAverageStayTime(int slot) {
            List<Long> durations = slotStayTimes.getOrDefault(slot, Collections.emptyList());
            if (durations.isEmpty()) return 0;
            long sum = 0;
            for (long d : durations) sum += d;
            return (int) (sum / durations.size());
        }

        public int getActionsPerMinute() {
            return actionTimes.size();
        }
    }
}

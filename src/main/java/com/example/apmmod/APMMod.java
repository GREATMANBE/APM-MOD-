package com.example.apmmod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = APMMod.MODID, name = APMMod.NAME, version = APMMod.VERSION, acceptedMinecraftVersions = "[1.8.9]")
public class APMMod {

    public static final String MODID = "apmmod";
    public static final String NAME = "APM Configurable Display";
    public static final String VERSION = "1.4";

    private int previousSlot = -1;
    private long lastSwitchTime = -1;

    private static APMMod instance;

    private final APMTracker apmTracker = new APMTracker();

    private int posX = 5;
    private int posY = 5;
    private int fontColor = 0xFFFFFF;

    private String displayText = "APM: %d";

    private boolean showAPM = true;
    private boolean showSlotTimes = true;
    private boolean modEnabled = true;

    private static final KeyBinding toggleOverlayKey = new KeyBinding("Toggle APM Overlay", Keyboard.KEY_F6, "APM Config");
    private static final KeyBinding toggleModKey = new KeyBinding("Toggle Mod Display", Keyboard.KEY_B, "APM Config");

    private File configFile;

    public boolean isInOverlayMode = false;

    public APMMod() {
        instance = this;
    }

    public static APMMod getInstance() {
        return instance;
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(toggleOverlayKey);
        ClientRegistry.registerKeyBinding(toggleModKey);
        this.configFile = new File(Minecraft.getMinecraft().mcDataDir, "apmmod.cfg");
        MinecraftForge.EVENT_BUS.register(this);

        // Register command "/apm"
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return "apm";
            }
        
            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/apm - toggle APM overlay";
            }
        
            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                Minecraft.getMinecraft().displayGuiScreen(new APMMod.APMOverlayGui());
                APMMod.this.isInOverlayMode = true;
            }
        
            @Override
            public int getRequiredPermissionLevel() {
                return 0;
            }
        });

        loadConfig();
    }

    private void onHotbarSwitch(int currentSlot) {
        if (currentSlot < 2 || currentSlot > 4) return;

        long currentTime = System.currentTimeMillis();

        if (previousSlot != -1 && lastSwitchTime != -1) {
            long interval = currentTime - lastSwitchTime;
            System.out.println("Switched from slot " + previousSlot + " to " + currentSlot +
                    " | Interval: " + interval + " ms");
        }

        previousSlot = currentSlot;
        lastSwitchTime = currentTime;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        int currentSlot = mc.thePlayer.inventory.currentItem;
        if (currentSlot != previousSlot) {
            onHotbarSwitch(currentSlot);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) return;

        int key = Keyboard.getEventKey();

        // Map keys 2, 3, 4 to Minecraft hotbar slots 2, 3, 4 for APM tracking
        if (key == Keyboard.KEY_2 || key == Keyboard.KEY_3 || key == Keyboard.KEY_4) {
            apmTracker.recordAction(key);
        }

        if (key == toggleOverlayKey.getKeyCode()) {
            Minecraft.getMinecraft().displayGuiScreen(new APMOverlayGui());
            isInOverlayMode = true;
        }

        if (key == toggleModKey.getKeyCode()) {
            modEnabled = !modEnabled;
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Display " + (modEnabled ? "ON" : "OFF")));
            saveConfig();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof APMOverlayGui)) {
            isInOverlayMode = false;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!modEnabled || isInOverlayMode) return;

        Minecraft mc = Minecraft.getMinecraft();

        int offset = 0;

        if (showAPM) {
            String display = String.format(displayText, apmTracker.getZFrontierStyleAPM());
            mc.fontRendererObj.drawString(display, posX, posY + offset, fontColor);
            offset += 10;
        }

        if (showSlotTimes) {
            for (int slot = 2; slot <= 4; slot++) {
                int stay = apmTracker.getAverageStayTime(slot);
                String stayText = stay > 999 ? "999+" : String.valueOf(stay);
                String avgText = String.format("Slot %d: %sms", slot, stayText);
                mc.fontRendererObj.drawString(avgText, posX, posY + offset, fontColor);
                offset += 10;
            }
        }
    }

    private void loadConfig() {
        try {
            if (!configFile.exists()) return;
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(configFile);
            props.load(in);
            in.close();

            posX = Integer.parseInt(props.getProperty("posX", "5"));
            posY = Integer.parseInt(props.getProperty("posY", "5"));
            fontColor = Integer.decode(props.getProperty("fontColor", "0xFFFFFF"));
            displayText = props.getProperty("displayText", "APM: %d");
            showAPM = Boolean.parseBoolean(props.getProperty("showAPM", "true"));
            showSlotTimes = Boolean.parseBoolean(props.getProperty("showSlotTimes", "true"));
            modEnabled = Boolean.parseBoolean(props.getProperty("modEnabled", "true"));

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        try {
            Properties props = new Properties();
            props.setProperty("posX", Integer.toString(posX));
            props.setProperty("posY", Integer.toString(posY));
            props.setProperty("fontColor", String.format("0x%06X", fontColor));
            props.setProperty("displayText", displayText);
            props.setProperty("showAPM", Boolean.toString(showAPM));
            props.setProperty("showSlotTimes", Boolean.toString(showSlotTimes));
            props.setProperty("modEnabled", Boolean.toString(modEnabled));

            FileOutputStream out = new FileOutputStream(configFile);
            props.store(out, "APM Mod Config");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class APMTracker {
        private final Queue<Long> timestamps = new LinkedList<>();
        private final Map<Integer, List<Long>> slotStayDurations = new HashMap<>();
        private int lastSlot = -1;
        private long lastSwitchTime = 0L;

        public void recordAction(int keyCode) {
            long now = System.currentTimeMillis();
            timestamps.add(now);

            // Map keys 2,3,4 to slots 2,3,4 respectively
            int slot = keyCode - Keyboard.KEY_1; // Keyboard.KEY_1 = 2, so this makes KEY_2 -> slot 1, etc. Need adjustment.

            // Correct slot calculation:
            // Keyboard.KEY_1 = 2, so slot = keyCode - 1, but you want slot 2,3,4 for keys 2,3,4
            // So slot = keyCode (2,3,4) directly is fine, or:
            slot = keyCode; // Use keyCode directly for slot, since you only track slots 2-4

            if (slot >= Keyboard.KEY_2 && slot <= Keyboard.KEY_4) {
                int slotNumber = slot; // Keys 2,3,4 directly used as slot numbers (2,3,4)

                if (lastSlot != -1 && lastSlot != slotNumber) {
                    long duration = now - lastSwitchTime;
                    slotStayDurations.putIfAbsent(lastSlot, new ArrayList<>());
                    List<Long> durations = slotStayDurations.get(lastSlot);
                    durations.add(duration);
                    if (durations.size() > 10) durations.remove(0);
                }
                lastSlot = slotNumber;
                lastSwitchTime = now;
            }
        }

        public int getAverageStayTime(int slot) {
            List<Long> durations = slotStayDurations.getOrDefault(slot, Collections.emptyList());
            if (durations.isEmpty()) return 0;
            long sum = 0L;
            for (Long d : durations) sum += d;
            return (int) (sum / durations.size());
        }

        public int getZFrontierStyleAPM() {
            long now = System.currentTimeMillis();
            long cutoff = now - 5000L;
            while (!timestamps.isEmpty() && timestamps.peek() < cutoff) {
                timestamps.poll();
            }
            int count = timestamps.size();
            if (count == 0) return 0;
            long last = 0L;
            for (Long t : timestamps) last = t;
            long secondsSinceLast = (now - last) / 1000L;
            return Math.max(12 * count - (int) secondsSinceLast, 0);
        }
    }

    private class APMOverlayGui extends GuiScreen {
        private GuiButton doneButton;
        private GuiButton resetButton;
        private boolean dragging = false;
        private int dragOffsetX;
        private int dragOffsetY;

        @Override
        public void initGui() {
            this.buttonList.clear();
            int centerX = this.width / 2;
            int bottomY = this.height - 30;
            this.doneButton = new GuiButton(0, centerX - 105, bottomY, 100, 20, "Done");
            this.resetButton = new GuiButton(1, centerX + 5, bottomY, 100, 20, "Reset");
            this.buttonList.add(doneButton);
            this.buttonList.add(resetButton);
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            APMMod mod = APMMod.getInstance();
            if (button.id == 0) {
                Minecraft.getMinecraft().displayGuiScreen(null);
                mod.saveConfig();
            } else if (button.id == 1) {
                mod.posX = 5;
                mod.posY = 5;
                mod.fontColor = 0xFFFFFF;
                mod.displayText = "APM: %d";
                mod.showAPM = true;
                mod.showSlotTimes = true;
                mod.modEnabled = true;
                mod.saveConfig();
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
            if (mouseButton == 0) {
                APMMod mod = APMMod.getInstance();
                // Drag box area roughly 100x30 at posX, posY
                if (mouseX >= mod.posX && mouseX <= mod.posX + 100 &&
                    mouseY >= mod.posY && mouseY <= mod.posY + 30) {
                    dragging = true;
                    dragOffsetX = mouseX - mod.posX;
                    dragOffsetY = mouseY - mod.posY;
                }
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
            if (dragging) {
                APMMod mod = APMMod.getInstance();
                mod.posX = mouseX - dragOffsetX;
                mod.posY = mouseY - dragOffsetY;
            }
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }

        @Override
        protected void mouseReleased(int mouseX, int mouseY, int state) {
            dragging = false;
            super.mouseReleased(mouseX, mouseY, state);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            APMMod mod = APMMod.getInstance();
            int offset = 0;

            drawCenteredString(fontRendererObj, "Click \"Done\" to save your current HUD position settings.", width / 2, height / 2 - 20, 0xFFFFFF);

            fontRendererObj.drawString("APM: XXX", mod.posX, mod.posY + offset, mod.fontColor);
            offset += 10;
            for (int slot = 2; slot <= 4; slot++) {
                fontRendererObj.drawString(String.format("Slot %d: ---+ms", slot), mod.posX, mod.posY + offset, mod.fontColor);
                offset += 10;
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}

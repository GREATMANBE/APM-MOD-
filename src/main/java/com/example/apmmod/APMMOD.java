package com.example.apmmod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
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
import org.lwjgl.input.Keyboard;

@Mod(modid = "apmmod", name = "APM Configurable Display", version = "1.4", acceptedMinecraftVersions = "[1.8.9]")
public class APMMod {

  private int previousSlot = -1;
  private long lastSwitchTime = -1;

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

  public static final String MODID = "apmmod";
  
  public static final String NAME = "APM Configurable Display";
  
  public static final String VERSION = "1.4";
  
  private static APMMod instance;
  
  private final APMTracker apmTracker = new APMTracker();
  
  private int posX = 5;
  
  private int posY = 5;
  
  private int fontColor = 16777215;
  
  private String displayText = "APM: %d";
  
  private boolean showAPM = true;
  
  private boolean showSlotTimes = true;
  
  private boolean modEnabled = true;
  
  private static final KeyBinding toggleOverlayKey = new KeyBinding("Toggle APM Overlay", 63, "APM Config");
  
  private static final KeyBinding toggleModKey = new KeyBinding("Toggle Mod Display", 66, "APM Config");
  
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
    this.configFile = new File((Minecraft.func_71410_x()).field_71412_D, "apmmod.cfg");
    MinecraftForge.EVENT_BUS.register(this);
    ClientCommandHandler.instance.func_71560_a((ICommand)new CommandBase() {
          public String func_71517_b() {
            return "apm";
          }
          
          public String func_71518_a(ICommandSender sender) {
            return "/apm - toggle APM overlay";
          }
          
          public void func_71515_b(ICommandSender sender, String[] args) {
            Minecraft.func_71410_x().func_147108_a(new APMMod.APMOverlayGui());
            APMMod.this.isInOverlayMode = true;
          }
          
          public int func_82362_a() {
            return 0;
          }
        });
    loadConfig();
  }
  
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) return;
  
      Minecraft mc = Minecraft.func_71410_x();
      if (mc.field_71439_g == null) return;
  
      int currentSlot = mc.field_71439_g.field_71071_by.field_70461_c;
      onHotbarSwitch(currentSlot);
  }

  
  public void onKeyInput(InputEvent.KeyInputEvent event) {
    if (Keyboard.getEventKeyState()) {
      int key = Keyboard.getEventKey();
      if (key == 3 || key == 4 || key == 5)
        this.apmTracker.recordAction(key); 
      if (key == toggleOverlayKey.func_151463_i()) {
        Minecraft.func_71410_x().func_147108_a(new APMOverlayGui());
        this.isInOverlayMode = true;
      } 
      if (key == toggleModKey.func_151463_i()) {
        this.modEnabled = !this.modEnabled;
        Minecraft.func_71410_x().field_71439_g.func_145747_a(
          new ChatComponentText("Display " + (this.modEnabled ? ": ON" : ": OFF"))
        );
        saveConfig();
      } 
    } 
  }
  
  @SubscribeEvent
  public void onGuiOpen(GuiOpenEvent event) {
    if (!(event.gui instanceof APMOverlayGui))
      this.isInOverlayMode = false; 
  }
  
  @SubscribeEvent
  public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
    if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !this.modEnabled || this.isInOverlayMode)
      return; 
    Minecraft mc = Minecraft.func_71410_x();
    int offset = 0;
    if (this.modEnabled) {
      String display = String.format(this.displayText, new Object[] { Integer.valueOf(this.apmTracker.getZFrontierStyleAPM()) });
      mc.field_71466_p.func_175063_a(display, this.posX, (this.posY + offset), this.fontColor);
      offset += 10;
      for (int slot = 2; slot <= 4; slot++) {
        int stay = this.apmTracker.getAverageStayTime(slot);
        String stayText = (stay > 999) ? "999+" : (stay + "");
        String avgText = String.format("Slot %d: %sms", new Object[] { Integer.valueOf(slot), stayText });
        mc.field_71466_p.func_175063_a(avgText, this.posX, (this.posY + offset), this.fontColor);
        offset += 10;
      } 
    } 
  }
  
  private void loadConfig() {
    try {
      if (!this.configFile.exists())
        return; 
      Properties props = new Properties();
      FileInputStream in = new FileInputStream(this.configFile);
      props.load(in);
      in.close();
      this.posX = Integer.parseInt(props.getProperty("posX", "5"));
      this.posY = Integer.parseInt(props.getProperty("posY", "5"));
      this.fontColor = Integer.decode(props.getProperty("fontColor", "0xFFFFFF")).intValue();
      this.displayText = props.getProperty("displayText", "APM: %d");
      this.showAPM = Boolean.parseBoolean(props.getProperty("showAPM", "true"));
      this.showSlotTimes = Boolean.parseBoolean(props.getProperty("showSlotTimes", "true"));
      this.modEnabled = Boolean.parseBoolean(props.getProperty("modEnabled", "true"));
    } catch (IOException|NumberFormatException iOException) {}
  }
  
  private void saveConfig() {
    try {
      Properties props = new Properties();
      props.setProperty("posX", Integer.toString(this.posX));
      props.setProperty("posY", Integer.toString(this.posY));
      props.setProperty("fontColor", String.format("0x%06X", new Object[] { Integer.valueOf(this.fontColor) }));
      props.setProperty("displayText", this.displayText);
      props.setProperty("showAPM", Boolean.toString(this.showAPM));
      props.setProperty("showSlotTimes", Boolean.toString(this.showSlotTimes));
      props.setProperty("modEnabled", Boolean.toString(this.modEnabled));
      FileOutputStream out = new FileOutputStream(this.configFile);
      props.store(out, "APM Mod Config");
      out.close();
    } catch (IOException iOException) {}
  }
  
  private static class APMTracker {
    private final Queue<Long> timestamps = new LinkedList<>();
    
    private final Map<Integer, List<Long>> slotStayDurations = new HashMap<>();
    
    private int lastSlot = -1;
    
    private long lastSwitchTime = 0L;
    
    public void recordAction(int keyCode) {
      long now = System.currentTimeMillis();
      this.timestamps.add(Long.valueOf(now));
      int slot = keyCode - 2 + 1;
      if (slot >= 2 && slot <= 4) {
        if (this.lastSlot != -1 && this.lastSlot != slot) {
          long duration = now - this.lastSwitchTime;
          this.slotStayDurations.putIfAbsent(Integer.valueOf(this.lastSlot), new ArrayList<>());
          List<Long> durations = this.slotStayDurations.get(Integer.valueOf(this.lastSlot));
          durations.add(Long.valueOf(duration));
          if (durations.size() > 10)
            durations.remove(0); 
        } 
        this.lastSlot = slot;
        this.lastSwitchTime = now;
      } 
    }
    
    public int getAverageStayTime(int slot) {
      List<Long> durations = this.slotStayDurations.getOrDefault(Integer.valueOf(slot), Collections.emptyList());
      if (durations.isEmpty())
        return 0; 
      long sum = 0L;
      for (Iterator<Long> iterator = durations.iterator(); iterator.hasNext(); ) {
        long d = ((Long)iterator.next()).longValue();
        sum += d;
      } 
      return (int)(sum / durations.size());
    }
    
    public int getZFrontierStyleAPM() {
      long now = System.currentTimeMillis();
      long cutoff = now - 5000L;
      while (!this.timestamps.isEmpty() && ((Long)this.timestamps.peek()).longValue() < cutoff)
        this.timestamps.poll(); 
      int count = this.timestamps.size();
      if (count == 0)
        return 0; 
      long last = 0L;
      for (Iterator<Long> iterator = this.timestamps.iterator(); iterator.hasNext(); last = t = ((Long)iterator.next()).longValue());
      long secondsSinceLast = (now - last) / 1000L;
      return Math.max(12 * count - (int)secondsSinceLast, 0);
    }
    
    private APMTracker() {}
  }
  
  private static class APMOverlayGui extends GuiScreen {
    private GuiButton doneButton;
    
    private GuiButton resetButton;
    
    private boolean dragging = false;
    
    private int dragOffsetX;
    
    private int dragOffsetY;
    
    public void func_73866_w_() {
      this.field_146292_n.clear();
      this.doneButton = new GuiButton(0, this.field_146294_l / 2 - 105, this.field_146295_m - 30, 100, 20, "Done");
      this.resetButton = new GuiButton(1, this.field_146294_l / 2 + 5, this.field_146295_m - 30, 100, 20, "Reset");
      this.field_146292_n.add(this.doneButton);
      this.field_146292_n.add(this.resetButton);
    }
    
    protected void func_146284_a(GuiButton button) {
      APMMod mod = APMMod.getInstance();
      if (button.field_146127_k == 0) {
        Minecraft.func_71410_x().func_147108_a(null);
        mod.saveConfig();
      } else if (button.field_146127_k == 1) {
        mod.posX = 5;
        mod.posY = 5;
        mod.fontColor = 16777215;
        mod.displayText = "APM: %d";
        mod.showAPM = true;
        mod.showSlotTimes = true;
        mod.modEnabled = true;
        mod.saveConfig();
      } 
    }
    
    protected void func_73864_a(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && mouseX >= (APMMod.getInstance()).posX && mouseX <= (APMMod.getInstance()).posX + 100 && mouseY >= (APMMod.getInstance()).posY && mouseY <= (APMMod.getInstance()).posY + 30) {
        this.dragging = true;
        this.dragOffsetX = mouseX - (APMMod.getInstance()).posX;
        this.dragOffsetY = mouseY - (APMMod.getInstance()).posY;
      } 
      try {
        super.func_73864_a(mouseX, mouseY, mouseButton);
      } catch (IOException e) {
        e.printStackTrace();
      } 
    }
    
    protected void func_146273_a(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
      if (this.dragging) {
        (APMMod.getInstance()).posX = mouseX - this.dragOffsetX;
        (APMMod.getInstance()).posY = mouseY - this.dragOffsetY;
      } 
      super.func_146273_a(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }
    
    protected void func_146286_b(int mouseX, int mouseY, int state) {
      this.dragging = false;
      super.func_146286_b(mouseX, mouseY, state);
    }
    
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
      func_146276_q_();
      APMMod mod = APMMod.getInstance();
      int offset = 0;
      func_73732_a(this.field_146289_q, "Click \"Done\" to save your current HUD position settings.", this.field_146294_l / 2, this.field_146295_m / 2 - 20, 16777215);
      this.field_146289_q.func_175063_a("APM: XXX", mod.posX, (mod.posY + offset), mod.fontColor);
      offset += 10;
      for (int slot = 2; slot <= 4; slot++) {
        this.field_146289_q.func_175063_a(String.format("Slot %d: ---+ms", new Object[] { Integer.valueOf(slot) }), mod.posX, (mod.posY + offset), mod.fontColor);
        offset += 10;
      } 
      super.func_73863_a(mouseX, mouseY, partialTicks);
    }
    
    public boolean func_73868_f() {
      return false;
    }
    
    private APMOverlayGui() {}
  }
}

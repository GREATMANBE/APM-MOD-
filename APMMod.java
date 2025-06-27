/*     */ package com.example.apmmod;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.FileOutputStream;
/*     */ import java.io.IOException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedList;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Properties;
/*     */ import java.util.Queue;
/*     */ import net.minecraft.client.Minecraft;
/*     */ import net.minecraft.client.gui.GuiButton;
/*     */ import net.minecraft.client.gui.GuiScreen;
/*     */ import net.minecraft.client.settings.KeyBinding;
/*     */ import net.minecraft.command.CommandBase;
/*     */ import net.minecraft.command.ICommand;
/*     */ import net.minecraft.command.ICommandSender;
/*     */ import net.minecraft.util.ChatComponentText;
/*     */ import net.minecraft.util.IChatComponent;
/*     */ import net.minecraftforge.client.ClientCommandHandler;
/*     */ import net.minecraftforge.client.event.GuiOpenEvent;
/*     */ import net.minecraftforge.client.event.RenderGameOverlayEvent;
/*     */ import net.minecraftforge.common.MinecraftForge;
/*     */ import net.minecraftforge.fml.client.registry.ClientRegistry;
/*     */ import net.minecraftforge.fml.common.Mod;
/*     */ import net.minecraftforge.fml.common.Mod.EventHandler;
/*     */ import net.minecraftforge.fml.common.event.FMLInitializationEvent;
/*     */ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
/*     */ import net.minecraftforge.fml.common.gameevent.InputEvent;
/*     */ import org.lwjgl.input.Keyboard;
/*     */ 
/*     */ @Mod(modid = "apmmod", name = "APM Configurable Display", version = "1.4", acceptedMinecraftVersions = "[1.8.9]")
/*     */ public class APMMod {
/*     */   public static final String MODID = "apmmod";
/*     */   
/*     */   public static final String NAME = "APM Configurable Display";
/*     */   
/*     */   public static final String VERSION = "1.4";
/*     */   
/*     */   private static APMMod instance;
/*     */   
/*  32 */   private final APMTracker apmTracker = new APMTracker();
/*     */   
/*  33 */   private int posX = 5;
/*     */   
/*  34 */   private int posY = 5;
/*     */   
/*  35 */   private int fontColor = 16777215;
/*     */   
/*  36 */   private String displayText = "APM: %d";
/*     */   
/*     */   private boolean showAPM = true;
/*     */   
/*     */   private boolean showSlotTimes = true;
/*     */   
/*     */   private boolean modEnabled = true;
/*     */   
/*  40 */   private static final KeyBinding toggleOverlayKey = new KeyBinding("Toggle APM Overlay", 63, "APM Config");
/*     */   
/*  41 */   private static final KeyBinding toggleModKey = new KeyBinding("Toggle Mod Display", 66, "APM Config");
/*     */   
/*     */   private File configFile;
/*     */   
/*     */   public boolean isInOverlayMode = false;
/*     */   
/*     */   public APMMod() {
/*  46 */     instance = this;
/*     */   }
/*     */   
/*     */   public static APMMod getInstance() {
/*  50 */     return instance;
/*     */   }
/*     */   
/*     */   @EventHandler
/*     */   public void init(FMLInitializationEvent event) {
/*  55 */     ClientRegistry.registerKeyBinding(toggleOverlayKey);
/*  56 */     ClientRegistry.registerKeyBinding(toggleModKey);
/*  57 */     this.configFile = new File((Minecraft.func_71410_x()).field_71412_D, "apmmod.cfg");
/*  58 */     MinecraftForge.EVENT_BUS.register(this);
/*  59 */     ClientCommandHandler.instance.func_71560_a((ICommand)new CommandBase() {
/*     */           public String func_71517_b() {
/*  62 */             return "apm";
/*     */           }
/*     */           
/*     */           public String func_71518_a(ICommandSender sender) {
/*  67 */             return "/apm - toggle APM overlay";
/*     */           }
/*     */           
/*     */           public void func_71515_b(ICommandSender sender, String[] args) {
/*  72 */             Minecraft.func_71410_x().func_147108_a(new APMMod.APMOverlayGui());
/*  73 */             APMMod.this.isInOverlayMode = true;
/*     */           }
/*     */           
/*     */           public int func_82362_a() {
/*  78 */             return 0;
/*     */           }
/*     */         });
/*  81 */     loadConfig();
/*     */   }
/*     */   
/*     */   @SubscribeEvent
/*     */   public void onKeyInput(InputEvent.KeyInputEvent event) {
/*  86 */     if (Keyboard.getEventKeyState()) {
/*  87 */       int key = Keyboard.getEventKey();
/*  88 */       if (key == 3 || key == 4 || key == 5)
/*  89 */         this.apmTracker.recordAction(key); 
/*  91 */       if (key == toggleOverlayKey.func_151463_i()) {
/*  92 */         Minecraft.func_71410_x().func_147108_a(new APMOverlayGui());
/*  93 */         this.isInOverlayMode = true;
/*     */       } 
/*  95 */       if (key == toggleModKey.func_151463_i()) {
/*  96 */         this.modEnabled = !this.modEnabled;
/*  97 */         (Minecraft.func_71410_x()).field_71439_g.func_145747_a((IChatComponent)new ChatComponentText("§eAPM/Slot Display " + (this.modEnabled ? "§aON" : "§cOFF")));
/*  98 */         saveConfig();
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   @SubscribeEvent
/*     */   public void onGuiOpen(GuiOpenEvent event) {
/* 105 */     if (!(event.gui instanceof APMOverlayGui))
/* 106 */       this.isInOverlayMode = false; 
/*     */   }
/*     */   
/*     */   @SubscribeEvent
/*     */   public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
/* 112 */     if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !this.modEnabled || this.isInOverlayMode)
/*     */       return; 
/* 113 */     Minecraft mc = Minecraft.func_71410_x();
/* 114 */     int offset = 0;
/* 115 */     if (this.modEnabled) {
/* 116 */       String display = String.format(this.displayText, new Object[] { Integer.valueOf(this.apmTracker.getZFrontierStyleAPM()) });
/* 117 */       mc.field_71466_p.func_175063_a(display, this.posX, (this.posY + offset), this.fontColor);
/* 118 */       offset += 10;
/* 119 */       for (int slot = 2; slot <= 4; slot++) {
/* 120 */         int stay = this.apmTracker.getAverageStayTime(slot);
/* 121 */         String stayText = (stay > 999) ? "999+" : (stay + "");
/* 122 */         String avgText = String.format("Slot %d: %sms", new Object[] { Integer.valueOf(slot), stayText });
/* 123 */         mc.field_71466_p.func_175063_a(avgText, this.posX, (this.posY + offset), this.fontColor);
/* 124 */         offset += 10;
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private void loadConfig() {
/*     */     try {
/* 131 */       if (!this.configFile.exists())
/*     */         return; 
/* 132 */       Properties props = new Properties();
/* 133 */       FileInputStream in = new FileInputStream(this.configFile);
/* 134 */       props.load(in);
/* 135 */       in.close();
/* 136 */       this.posX = Integer.parseInt(props.getProperty("posX", "5"));
/* 137 */       this.posY = Integer.parseInt(props.getProperty("posY", "5"));
/* 138 */       this.fontColor = Integer.decode(props.getProperty("fontColor", "0xFFFFFF")).intValue();
/* 139 */       this.displayText = props.getProperty("displayText", "APM: %d");
/* 140 */       this.showAPM = Boolean.parseBoolean(props.getProperty("showAPM", "true"));
/* 141 */       this.showSlotTimes = Boolean.parseBoolean(props.getProperty("showSlotTimes", "true"));
/* 142 */       this.modEnabled = Boolean.parseBoolean(props.getProperty("modEnabled", "true"));
/* 143 */     } catch (IOException|NumberFormatException iOException) {}
/*     */   }
/*     */   
/*     */   private void saveConfig() {
/*     */     try {
/* 148 */       Properties props = new Properties();
/* 149 */       props.setProperty("posX", Integer.toString(this.posX));
/* 150 */       props.setProperty("posY", Integer.toString(this.posY));
/* 151 */       props.setProperty("fontColor", String.format("0x%06X", new Object[] { Integer.valueOf(this.fontColor) }));
/* 152 */       props.setProperty("displayText", this.displayText);
/* 153 */       props.setProperty("showAPM", Boolean.toString(this.showAPM));
/* 154 */       props.setProperty("showSlotTimes", Boolean.toString(this.showSlotTimes));
/* 155 */       props.setProperty("modEnabled", Boolean.toString(this.modEnabled));
/* 156 */       FileOutputStream out = new FileOutputStream(this.configFile);
/* 157 */       props.store(out, "APM Mod Config");
/* 158 */       out.close();
/* 159 */     } catch (IOException iOException) {}
/*     */   }
/*     */   
/*     */   private static class APMTracker {
/* 163 */     private final Queue<Long> timestamps = new LinkedList<>();
/*     */     
/* 164 */     private final Map<Integer, List<Long>> slotStayDurations = new HashMap<>();
/*     */     
/* 165 */     private int lastSlot = -1;
/*     */     
/* 166 */     private long lastSwitchTime = 0L;
/*     */     
/*     */     public void recordAction(int keyCode) {
/* 169 */       long now = System.currentTimeMillis();
/* 170 */       this.timestamps.add(Long.valueOf(now));
/* 171 */       int slot = keyCode - 2 + 1;
/* 172 */       if (slot >= 2 && slot <= 4) {
/* 173 */         if (this.lastSlot != -1 && this.lastSlot != slot) {
/* 174 */           long duration = now - this.lastSwitchTime;
/* 175 */           this.slotStayDurations.putIfAbsent(Integer.valueOf(this.lastSlot), new ArrayList<>());
/* 176 */           List<Long> durations = this.slotStayDurations.get(Integer.valueOf(this.lastSlot));
/* 177 */           durations.add(Long.valueOf(duration));
/* 178 */           if (durations.size() > 10)
/* 178 */             durations.remove(0); 
/*     */         } 
/* 180 */         this.lastSlot = slot;
/* 181 */         this.lastSwitchTime = now;
/*     */       } 
/*     */     }
/*     */     
/*     */     public int getAverageStayTime(int slot) {
/* 186 */       List<Long> durations = this.slotStayDurations.getOrDefault(Integer.valueOf(slot), Collections.emptyList());
/* 187 */       if (durations.isEmpty())
/* 187 */         return 0; 
/* 188 */       long sum = 0L;
/* 189 */       for (Iterator<Long> iterator = durations.iterator(); iterator.hasNext(); ) {
/* 189 */         long d = ((Long)iterator.next()).longValue();
/* 189 */         sum += d;
/*     */       } 
/* 190 */       return (int)(sum / durations.size());
/*     */     }
/*     */     
/*     */     public int getZFrontierStyleAPM() {
/* 194 */       long now = System.currentTimeMillis();
/* 195 */       long cutoff = now - 5000L;
/* 196 */       while (!this.timestamps.isEmpty() && ((Long)this.timestamps.peek()).longValue() < cutoff)
/* 197 */         this.timestamps.poll(); 
/* 199 */       int count = this.timestamps.size();
/* 200 */       if (count == 0)
/* 200 */         return 0; 
/* 201 */       long last = 0L;
/* 202 */       for (Iterator<Long> iterator = this.timestamps.iterator(); iterator.hasNext(); last = t = ((Long)iterator.next()).longValue());
/* 203 */       long secondsSinceLast = (now - last) / 1000L;
/* 204 */       return Math.max(12 * count - (int)secondsSinceLast, 0);
/*     */     }
/*     */     
/*     */     private APMTracker() {}
/*     */   }
/*     */   
/*     */   private static class APMOverlayGui extends GuiScreen {
/*     */     private GuiButton doneButton;
/*     */     
/*     */     private GuiButton resetButton;
/*     */     
/*     */     private boolean dragging = false;
/*     */     
/*     */     private int dragOffsetX;
/*     */     
/*     */     private int dragOffsetY;
/*     */     
/*     */     public void func_73866_w_() {
/* 217 */       this.field_146292_n.clear();
/* 218 */       this.doneButton = new GuiButton(0, this.field_146294_l / 2 - 105, this.field_146295_m - 30, 100, 20, "Done");
/* 219 */       this.resetButton = new GuiButton(1, this.field_146294_l / 2 + 5, this.field_146295_m - 30, 100, 20, "Reset");
/* 220 */       this.field_146292_n.add(this.doneButton);
/* 221 */       this.field_146292_n.add(this.resetButton);
/*     */     }
/*     */     
/*     */     protected void func_146284_a(GuiButton button) {
/* 226 */       APMMod mod = APMMod.getInstance();
/* 227 */       if (button.field_146127_k == 0) {
/* 228 */         Minecraft.func_71410_x().func_147108_a(null);
/* 229 */         mod.saveConfig();
/* 230 */       } else if (button.field_146127_k == 1) {
/* 231 */         mod.posX = 5;
/* 232 */         mod.posY = 5;
/* 233 */         mod.fontColor = 16777215;
/* 234 */         mod.displayText = "APM: %d";
/* 235 */         mod.showAPM = true;
/* 236 */         mod.showSlotTimes = true;
/* 237 */         mod.modEnabled = true;
/* 238 */         mod.saveConfig();
/*     */       } 
/*     */     }
/*     */     
/*     */     protected void func_73864_a(int mouseX, int mouseY, int mouseButton) {
/* 244 */       if (mouseButton == 0 && mouseX >= (APMMod.getInstance()).posX && mouseX <= (APMMod.getInstance()).posX + 100 && mouseY >= (APMMod.getInstance()).posY && mouseY <= (APMMod.getInstance()).posY + 30) {
/* 245 */         this.dragging = true;
/* 246 */         this.dragOffsetX = mouseX - (APMMod.getInstance()).posX;
/* 247 */         this.dragOffsetY = mouseY - (APMMod.getInstance()).posY;
/*     */       } 
/*     */       try {
/* 250 */         super.func_73864_a(mouseX, mouseY, mouseButton);
/* 251 */       } catch (IOException e) {
/* 252 */         e.printStackTrace();
/*     */       } 
/*     */     }
/*     */     
/*     */     protected void func_146273_a(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
/* 259 */       if (this.dragging) {
/* 260 */         (APMMod.getInstance()).posX = mouseX - this.dragOffsetX;
/* 261 */         (APMMod.getInstance()).posY = mouseY - this.dragOffsetY;
/*     */       } 
/* 263 */       super.func_146273_a(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
/*     */     }
/*     */     
/*     */     protected void func_146286_b(int mouseX, int mouseY, int state) {
/* 268 */       this.dragging = false;
/* 269 */       super.func_146286_b(mouseX, mouseY, state);
/*     */     }
/*     */     
/*     */     public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
/* 274 */       func_146276_q_();
/* 275 */       APMMod mod = APMMod.getInstance();
/* 276 */       int offset = 0;
/* 277 */       func_73732_a(this.field_146289_q, "Click \"Done\" to save your current HUD position settings.", this.field_146294_l / 2, this.field_146295_m / 2 - 20, 16777215);
/* 278 */       this.field_146289_q.func_175063_a("APM: XXX", mod.posX, (mod.posY + offset), mod.fontColor);
/* 279 */       offset += 10;
/* 280 */       for (int slot = 2; slot <= 4; slot++) {
/* 281 */         this.field_146289_q.func_175063_a(String.format("Slot %d: ---+ms", new Object[] { Integer.valueOf(slot) }), mod.posX, (mod.posY + offset), mod.fontColor);
/* 282 */         offset += 10;
/*     */       } 
/* 284 */       super.func_73863_a(mouseX, mouseY, partialTicks);
/*     */     }
/*     */     
/*     */     public boolean func_73868_f() {
/* 288 */       return false;
/*     */     }
/*     */     
/*     */     private APMOverlayGui() {}
/*     */   }
/*     */ }


/* Location:              C:\Users\mural\OneDrive\Desktop\apmmod-1.0.jar!\com\example\apmmod\APMMod.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */
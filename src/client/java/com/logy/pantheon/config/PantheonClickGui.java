package com.logy.pantheon.config;

import com.logy.pantheon.config.gui.Category;
import com.logy.pantheon.config.gui.Module;
import com.logy.pantheon.config.gui.SettingDefinition;
import com.logy.pantheon.config.gui.util.ColorUtil;
import com.logy.pantheon.config.gui.widgets.BooleanWidget;
import com.logy.pantheon.config.gui.widgets.ButtonWidget;
import com.logy.pantheon.config.gui.widgets.SliderWidget;
import com.logy.pantheon.config.gui.widgets.TextWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PantheonClickGui extends Screen {

    private final List<Category> panels = new ArrayList<>();

    public PantheonClickGui(Screen parent) {
        super(Component.literal("Pantheon Config"));
        buildPanels();
    }

    private void buildPanels() {
        int x = 10;
        for (String categoryName : ModuleRegistry.getCategories()) {
            Category cat = new Category(categoryName, x, 10);
            for (ModuleRegistry.ModuleSchema schema : ModuleRegistry.getModulesByCategory(categoryName)) {
                ModuleConfig cfg = ModuleConfig.get(schema.id);
                Module module;
                boolean hasToggle = schema.settings != null && schema.settings.stream()
                        .anyMatch(s -> "enabled".equals(s.id) && s.type == SettingDefinition.SettingType.BOOLEAN);

                if (hasToggle) {
                    module = new Module(schema.name,
                            () -> ModuleRegistry.isEnabled(schema.id),
                            v -> ModuleRegistry.setEnabled(schema.id, v));
                } else {
                    module = new Module(schema.name);
                }

                if (schema.settings != null) {
                    for (SettingDefinition def : schema.settings) {
                        if ("enabled".equals(def.id) && def.type == SettingDefinition.SettingType.BOOLEAN) continue;
                        if (def.name == null || def.name.isEmpty()) continue;
                        switch (def.type) {
                            case BOOLEAN -> {
                                module.add(new BooleanWidget(def.name,
                                        () -> cfg.getBool(def.id),
                                        v -> cfg.set(def.id, v)));
                            }
                            case SLIDER -> {
                                module.add(new SliderWidget(def.name, def.min, def.max, def.step,
                                        () -> cfg.getInt(def.id),
                                        v -> cfg.set(def.id, v)));
                            }
                            case TEXT -> {
                                module.add(new TextWidget(def.name,
                                        () -> cfg.getString(def.id),
                                        v -> cfg.set(def.id, v)));
                            }
                            case BUTTON -> {
                                Runnable action = ModuleRegistry.getButtonAction(schema.id, def.id);
                                if (action != null) {
                                    module.add(new ButtonWidget(def.name, action));
                                }
                            }
                        }
                    }
                }
                cat.add(module);
            }
            if (!cat.isEmpty()) {
                panels.add(cat);
                x += 190;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(g, mouseX, mouseY, deltaTicks);
        for (Category panel : panels) panel.tickDrag(mouseX, mouseY);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), ColorUtil.rgba(0, 0, 0, 140));
        for (Category panel : panels) {
            panel.render(g, this.font, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent btn, boolean bl) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked((int) (btn.x()), (int) (btn.y()), btn)) {
                if (panels.get(i).isDragging()) {
                    panels.add(panels.remove(i));
                }
                return true;
            }
        }
        return super.mouseClicked(btn, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent btn) {
        for (Category p : panels) p.mouseReleased(btn);
        return super.mouseReleased(btn);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        for (Category p : panels) if (p.keyPressed(event)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        for (Category p : panels) if (p.charTyped(event)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        ModuleConfig.saveAll();
        super.onClose();
    }
}

package me.cortex.voxy.client.config;

import me.cortex.voxy.client.ClientSessionEvents;
import me.cortex.voxy.client.core.IVoxyRenderSystemHolder;
import me.cortex.voxy.client.core.NormalRenderPipeline;
import me.cortex.voxy.client.core.SSAO;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class VoxyConfigScreen extends Screen {
    private final Screen parent;
    private final VoxyConfig config = VoxyConfig.CONFIG;
    private int rowY;

    public VoxyConfigScreen(Screen parent) {
        super(Component.literal("Voxy Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int left = Math.max(12, (this.width - 360) / 2);
        int contentWidth = Math.min(360, this.width - 24);
        int controlWidth = Math.min(180, contentWidth - 120);
        int controlX = left + contentWidth - controlWidth;
        this.rowY = Math.max(48, (this.height - 330) / 2);

        addToggle(left, controlX, controlWidth, "Enable Voxy", () -> config.enabled,
                value -> {
                    config.enabled = value;
                    if (value && ClientSessionEvents.inSession) {
                        VoxyCommon.createInstance();
                    } else if (!value) {
                        var holder = IVoxyRenderSystemHolder.getNullableHolder();
                        if (holder != null) holder.voxy$shutdownRenderer();
                        VoxyCommon.shutdownInstance();
                    }
                    save();
                    IrisUtil.reload();
                });

        addToggle(left, controlX, controlWidth, "Enable LOD rendering", () -> config.enableRendering,
                value -> {
                    config.enableRendering = value;
                    var holder = IVoxyRenderSystemHolder.getNullableHolder();
                    if (holder != null) {
                        if (value && config.enabled) holder.voxy$createRenderer();
                        else holder.voxy$shutdownRenderer();
                    }
                    save();
                    IrisUtil.reload();
                });

        addToggle(left, controlX, controlWidth, "World ingestion", () -> config.ingestEnabled,
                value -> { config.ingestEnabled = value; save(); });

        int maxThreads = Math.max(1, CpuLayout.getCoreCount());
        addSlider(left, controlX, controlWidth, "Service threads", 1, maxThreads,
                config.serviceThreads, value -> {
                    config.serviceThreads = (int) Math.round(value);
                    save();
                    var instance = VoxyCommon.getInstance();
                    if (instance != null) instance.updateDedicatedThreads();
                }, value -> Integer.toString((int) Math.round(value)));

        addSlider(left, controlX, controlWidth, "Render distance", 10, 1024,
                Math.max(10, Math.min(1024, config.sectionRenderDistance * 16.0)), value -> {
                    config.sectionRenderDistance = (float) value / 16.0f;
                    save();
                    var renderer = IVoxyRenderSystemHolder.getNullable();
                    if (renderer != null) renderer.setRenderDistance(config.sectionRenderDistance);
                }, value -> Integer.toString((int) Math.round(value * 2)));

        addSlider(left, controlX, controlWidth, "Subdivision size", 28, 256,
                Math.max(28, Math.min(256, config.subDivisionSize)), value -> {
                    config.subDivisionSize = (float) value;
                    save();
                }, value -> Integer.toString((int) Math.round(value)));

        addEnum(left, controlX, controlWidth, "Environmental fog",
                NormalRenderPipeline.FogMode.values(), config.getFogMode(),
                value -> { config.setFogMode(value); save(); IrisUtil.reload(); });

        addEnum(left, controlX, controlWidth, "SSAO mode",
                SSAO.SSAOMode.values(), config.getSSAOMode(),
                value -> { config.setSSAOMode(value); save(); IrisUtil.reload(); });

        addToggle(left, controlX, controlWidth, "Use Sodium builder threads",
                () -> !config.dontUseSodiumBuilderThreads,
                value -> {
                    config.dontUseSodiumBuilderThreads = !value;
                    save();
                    var instance = VoxyCommon.getInstance();
                    if (instance != null) instance.updateDedicatedThreads();
                });

        int buttonY = Math.min(this.height - 32, this.rowY + 9 * 34 + 12);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> this.onClose()).bounds(this.width / 2 - 100, buttonY, 200, 20).build());
    }

    private void addToggle(int labelX, int x, int width, String label,
                           java.util.function.Supplier<Boolean> getter, Consumer<Boolean> setter) {
        int y = this.rowY;
        this.addRenderableWidget(Button.builder(toggleText(label, getter.get()), button -> {
            boolean next = !getter.get();
            setter.accept(next);
            button.setMessage(toggleText(label, getter.get()));
        }).bounds(x, y, width, 22).build());
        this.rowY += 34;
    }

    private static Component toggleText(String label, boolean enabled) {
        return Component.literal(label + ": " + (enabled ? "ON" : "OFF"));
    }

    private void addSlider(int labelX, int x, int width, String label, double min, double max,
                           double initial, Consumer<Double> setter, Function<Double, String> formatter) {
        this.addRenderableWidget(new ConfigSlider(x, this.rowY, width, 22, label, min, max,
                initial, setter, formatter));
        this.rowY += 34;
    }

    private <T extends Enum<T>> void addEnum(int labelX, int x, int width, String label,
                                             T[] values, T current, Consumer<T> setter) {
        this.addRenderableWidget(Button.builder(enumText(label, current), button -> {
            int index = (currentIndex(values, current) + 1) % values.length;
            T next = values[index];
            setter.accept(next);
            button.setMessage(enumText(label, next));
            // Rebuild the page so subsequent presses advance from the newly selected value.
            this.init();
        }).bounds(x, this.rowY, width, 22).build());
        this.rowY += 34;
    }

    private static <T> int currentIndex(T[] values, T current) {
        for (int i = 0; i < values.length; i++) if (values[i] == current) return i;
        return 0;
    }

    private static Component enumText(String label, Enum<?> value) {
        return Component.literal(label + ": " + value.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    private void save() {
        this.config.save();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private static final class ConfigSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final Consumer<Double> setter;
        private final Function<Double, String> formatter;

        private ConfigSlider(int x, int y, int width, int height, String label,
                             double min, double max, double initial,
                             Consumer<Double> setter, Function<Double, String> formatter) {
            super(x, y, width, height, Component.empty(),
                    (Math.max(min, Math.min(max, initial)) - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            this.updateMessage();
        }

        private double actualValue() {
            return this.min + this.value * (this.max - this.min);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.label + ": " + this.formatter.apply(this.actualValue())));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(this.actualValue());
        }
    }
}

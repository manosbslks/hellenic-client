package gr.hellenic.client.modules;

import gr.hellenic.client.concurrent.AfterPlayerAiStepExecutor;
import gr.hellenic.client.concurrent.AfterSendPlayerPosExecutor;
import gr.hellenic.client.concurrent.PreRenderGuiExecutor;
import gr.hellenic.client.concurrent.TickEndExecutor;
import gr.hellenic.client.controllers.*;
import gr.hellenic.client.font.FontBackendHolders;
import gr.hellenic.client.modules.automation.*;
import gr.hellenic.client.modules.esp.*;
import gr.hellenic.client.modules.hacks.*;
import gr.hellenic.client.modules.scripting.*;
import gr.hellenic.client.modules.utilities.*;
import gr.hellenic.client.modules.visuals.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Modules {

    private static final Logger LOGGER = LogManager.getLogger(Modules.class);

    public static void register() {

        //Order dependent modules -> legacy method, use Event.event.add(function, priority) for new modules, use the priority value instead
        //==========================

        //===========================

        register(FakeRotation.instance);
        register(BlockEventsProcessor.instance);
        register(NetworkPacketsController.instance);
        register(SpeedCounterController.instance);
        register(BlockFinder.instance);
        register(PreRenderGuiExecutor.instance);

        register(AutoTotem.instance);
        register(AutoEat.instance);
        register(NoFall.instance);
        register(Scaffold.instance);

        register(LockInputsController.instance);
        register(AutoCraft.instance);
        register(BlockEsp.instance);
        register(EntityEsp.instance);
        register(ProjectilePath.instance);
        register(EndCityChunks.instance);
        register(AutoBucket.instance);
        register(WorldDownloadController.instance);
        register(EntityTitle.instance);
        register(ContainerButtonsController.instance);
        register(TeleportHackController.instance);
        register(WorldMarkers.instance);
        register(TpsCounterController.instance);
        register(BlockAutomation.instance);
        register(PlayerInfoController.instance);

        register(FlyHack.instance);
        register(FreeCam.instance);
        register(AutoFish.instance);
        register(ChunkOverlayController.instance);
        register(StatusOverlay.instance);
        register(AutoCriticals.instance);
        register(LightLevel.instance);
        register(ElytraFly.instance);
        register(AdvancedTooltips.instance);
        register(Zoom.instance);
        register(ShulkerTooltip.instance);
        register(ArmorOverlay.instance);
        register(Fog.instance);
        register(AutoAttack.instance);

        register(Exec.instance);
        register(VillagerRoller.instance);
        register(AutoHotbar.instance);
        register(AreaMine.instance);
        register(ServerPlugins.instance);
        register(BedrockBreaker.instance);
        register(RenderUtilities.instance);
        register(Containers.instance);
        register(AntiHunger.instance);
        register(Schematica.instance);
        register(AimAssist.instance);
        register(LockInputs.instance);
        register(LogoutSpots.instance);
        register(AutoTool.instance);
        register(AirPlace.instance);
        register(ContainerSummary.instance);

        register(TickEndExecutor.instance);

        // new order independent modules
        //==========================================
        register(AfterPlayerAiStepExecutor.instance);
        register(AfterSendPlayerPosExecutor.instance);
        register(KillAura.instance);

        register(SpearRange.instance);
        register(AutoStunner.instance);
        register(BreachSwap.instance);
        //===========================================

        FontBackendHolders.add(StatusOverlay.instance);
        FontBackendHolders.add(EntityTitle.instance);
        FontBackendHolders.add(WorldMarkers.instance);
    }

    public static void registerKeyBindings() {
        register(KeyBindingsController.instance);
        register(MenuController.instance);
        register(HudController.instance);
        register(SplashController.instance);
    }

    private static void register(Module module) {
        LOGGER.debug("Registered module {}", module.getClass().getName());
    }

    private static void register(Object instance) {
        LOGGER.debug("Registered controller {}", instance.getClass().getName());
    }
}
package gr.hellenic.client.webui;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.ScriptsController;
import gr.hellenic.client.modules.automation.VillagerRoller;
import com.zergatul.scripting.compiler.CompilationResult;

public class VillagerRollerCodeApi extends CodeApiBase<Runnable> {

    @Override
    public String getRoute() {
        return "villager-roller-code";
    }

    @Override
    protected CompilationResult compile(String code) {
        return ScriptsController.instance.compileVillagerRoller(code);
    }

    @Override
    protected void setCode(String code) {
        ConfigStore.instance.getConfig().villagerRollerConfig.code = code;
    }

    @Override
    protected void setProgram(Runnable program) {
        VillagerRoller.instance.setScript(program);
    }
}
package gr.hellenic.client.webui;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.ScriptsController;
import gr.hellenic.client.modules.hacks.KillAura;
import gr.hellenic.client.scripting.KillAuraFunction;
import com.zergatul.scripting.compiler.CompilationResult;

public class KillAuraCodeApi extends CodeApiBase<KillAuraFunction> {

    @Override
    public String getRoute() {
        return "kill-aura-code";
    }

    @Override
    protected CompilationResult compile(String code) {
        return ScriptsController.instance.compileKillAura(code);
    }

    @Override
    protected void setCode(String code) {
        ConfigStore.instance.getConfig().killAuraConfig.code = code;
    }

    @Override
    protected void setProgram(KillAuraFunction program) {
        KillAura.instance.setScript(program);
    }
}
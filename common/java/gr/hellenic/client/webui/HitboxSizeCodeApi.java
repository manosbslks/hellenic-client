package gr.hellenic.client.webui;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.ScriptsController;
import gr.hellenic.client.modules.hacks.HitboxSize;
import gr.hellenic.client.scripting.HitboxSizeFunction;
import com.zergatul.scripting.compiler.CompilationResult;

public class HitboxSizeCodeApi extends CodeApiBase<HitboxSizeFunction> {

    @Override
    public String getRoute() {
        return "hitbox-size-code";
    }

    @Override
    protected CompilationResult compile(String code) {
        return ScriptsController.instance.compileHitboxSize(code);
    }

    @Override
    protected void setCode(String code) {
        ConfigStore.instance.getConfig().hitboxSizeConfig.code = code;
    }

    @Override
    protected void setProgram(HitboxSizeFunction program) {
        HitboxSize.instance.setScript(program);
    }
}
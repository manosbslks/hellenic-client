package gr.hellenic.client.webui;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.ScriptsController;
import gr.hellenic.client.modules.scripting.BlockAutomation;
import gr.hellenic.client.scripting.events.BlockPosConsumer;
import com.zergatul.scripting.compiler.CompilationResult;

public class BlockAutomationCodeApi extends CodeApiBase<BlockPosConsumer> {

    @Override
    public String getRoute() {
        return "block-automation-code";
    }

    @Override
    protected CompilationResult compile(String code) {
        return ScriptsController.instance.compileBlockAutomation(code);
    }

    @Override
    protected void setCode(String code) {
        ConfigStore.instance.getConfig().blockAutomationConfig.code = code;
    }

    @Override
    protected void setProgram(BlockPosConsumer program) {
        BlockAutomation.instance.setScript(program);
    }
}
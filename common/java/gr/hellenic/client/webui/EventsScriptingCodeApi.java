package gr.hellenic.client.webui;

import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.controllers.ScriptsController;
import gr.hellenic.client.modules.scripting.EventsScripting;
import com.zergatul.scripting.compiler.CompilationResult;

public class EventsScriptingCodeApi extends CodeApiBase<Runnable> {

    @Override
    public String getRoute() {
        return "events-scripting-code";
    }

    @Override
    protected CompilationResult compile(String code) {
        return ScriptsController.instance.compileEvents(code);
    }

    @Override
    protected void setCode(String code) {
        ConfigStore.instance.getConfig().eventsScriptingConfig.code = code;
    }

    @Override
    protected void setProgram(Runnable program) {
        EventsScripting.instance.setScript(program);
    }
}
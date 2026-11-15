package gr.hellenic.client.webui;

import gr.hellenic.client.concurrent.TickEndExecutor;
import gr.hellenic.client.configs.ConfigStore;
import gr.hellenic.client.configs.EntityEspConfig;
import gr.hellenic.client.controllers.ScriptsController;
import com.zergatul.scripting.compiler.CompilationResult;

public class EntityEspCodeApi extends ApiBase {

    @Override
    public String getRoute() {
        return "entity-esp-code";
    }

    @Override
    public String post(String json) throws ApiException {
        Request request = gson.fromJson(json, Request.class);

        EntityEspConfig config = ConfigStore.instance.getConfig().entities.configs.stream()
                .filter(c -> c.clazz == request.clazz)
                .findFirst()
                .orElse(null);
        if (config == null) {
            throw new ApiException("Cannot find entity config.", HttpResponseCodes.NOT_FOUND);
        }

        if (request.code == null || request.code.isBlank()) {
            TickEndExecutor.instance.execute(() -> {
                config.code = null;
                config.script = null;
                ConfigStore.instance.requestWrite();
            });
            return "{ \"ok\": true }";
        }

        CompilationResult result = ScriptsController.instance.compileEntityEsp(request.code);
        if (result.getProgram() != null) {
            TickEndExecutor.instance.execute(() -> {
                config.code = request.code;
                config.script = result.getProgram();
                ConfigStore.instance.requestWrite();
            });
            return "{ \"ok\": true }";
        } else {
            return gson.toJson(result.getDiagnostics());
        }
    }

    public record Request(Class<?> clazz, String code) {}
}
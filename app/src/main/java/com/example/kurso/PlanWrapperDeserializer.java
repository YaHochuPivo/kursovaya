package com.example.kurso;

import com.example.kurso.TaskItem;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PlanWrapperDeserializer implements JsonDeserializer<PlanWrapper> {
    @Override
    public PlanWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String id = obj.has("id") ? obj.get("id").getAsString() : null;
        List<TaskItem> tasks = new ArrayList<>();

        if (obj.has("tasks") && obj.get("tasks").isJsonArray()) {
            for (JsonElement taskElement : obj.getAsJsonArray("tasks")) {
                TaskItem task = context.deserialize(taskElement, TaskItem.class);
                tasks.add(task);
            }
        }

        return new PlanWrapper(id, tasks);
    }
}

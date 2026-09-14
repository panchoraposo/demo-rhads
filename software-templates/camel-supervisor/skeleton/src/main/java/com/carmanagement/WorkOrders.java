package com.carmanagement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.camel.BindToRegistry;

@BindToRegistry("workOrders")
public class WorkOrders {

    private static final Path DIR = Path.of("work-orders");

    public List<Map<String, Object>> list() throws Exception {
        if (!Files.exists(DIR)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(DIR)) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .limit(25)
                    .toList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Path path : files) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("file", path.getFileName().toString());
                item.put("content", Files.readString(path));
                result.add(item);
            }
            return result;
        }
    }
}

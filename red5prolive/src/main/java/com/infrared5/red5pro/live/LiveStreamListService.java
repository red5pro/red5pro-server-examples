package com.infrared5.red5pro.live;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.red5.server.api.scope.IScope;
import org.springframework.core.io.Resource;

public class LiveStreamListService {

    // thread-safe in newer jdks
    private static SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy H:mm:ss", Locale.US);

    private final Red5ProLive app;

    public LiveStreamListService(Red5ProLive owner) {
        app = owner;
    }

    public List<String> getLiveStreams() {
        List<String> ret = app.getLiveStreams();
        return ret;
    }

    public Map<String, Map<String, Object>> getListOfAvailableFLVs() throws IOException {
        Map<String, Map<String, Object>> filesMap = new HashMap<>();
        IScope scope = app.getScope();
        if (scope != null) {
            addToMap(filesMap, scope.getResources("streams/*.flv"));
            addToMap(filesMap, scope.getResources("streams/*.mp4"));
        }
        return filesMap;
    }

    private String formatDate(Date date) {
        return formatter.format(date);
    }

    private void addToMap(Map<String, Map<String, Object>> filesMap, Resource[] files) throws IOException {
        if (files != null) {
            for (Resource flv : files) {
                File file = flv.getFile();
                Date lastModifiedDate = new Date(file.lastModified());
                String lastModified = formatDate(lastModifiedDate);
                String flvName = flv.getFile().getName();
                String flvBytes = Long.toString(file.length());

                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", flvName);
                fileInfo.put("lastModified", lastModified);
                fileInfo.put("size", flvBytes);
                filesMap.put(flvName, fileInfo);
            }
        }
    }

}

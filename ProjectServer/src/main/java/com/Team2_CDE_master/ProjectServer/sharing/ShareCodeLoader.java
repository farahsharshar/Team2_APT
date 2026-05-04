package com.Team2_CDE_master.ProjectServer.sharing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ShareCodeLoader implements ApplicationRunner {

    @Autowired
    private ShareCodeRepository repo;

    @Autowired
    private ShareRegistry registry;

    @Override
    public void run(ApplicationArguments args) {
        int count = 0;
        for (ShareCodeEntity entity : repo.findAll()) {
            registry.seed(entity.getDocId(), entity.getEditorCode(), entity.getViewerCode());
            count++;
        }
        if (count > 0) {
            System.out.println("[ShareCodeLoader] Restored " + count + " share code(s) from database.");
        }
    }
}

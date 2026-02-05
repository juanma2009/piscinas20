package com.bolsadeideas.springboot.app.models.service.workshop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChecklistSeeder implements CommandLineRunner {

    @Autowired
    private WorkshopService workshopService;

    @Override
    public void run(String... args) throws Exception {
        workshopService.seedTemplates();
    }
}

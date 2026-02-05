package com.bolsadeideas.springboot.app.controllers;

import com.bolsadeideas.springboot.app.models.entity.Pedido;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistInstance;
import com.bolsadeideas.springboot.app.models.entity.workshop.ChecklistItemInstance;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import com.bolsadeideas.springboot.app.models.service.workshop.WorkshopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/workshop")
public class WorkshopController {

    private static final Logger log = LoggerFactory.getLogger(WorkshopController.class);

    @Autowired
    private WorkshopService workshopService;

    @Autowired
    private IClienteService clienteService;

    @GetMapping("/checklist/{id}")
    public String checklist(@PathVariable(value = "id") Long id, Model model, RedirectAttributes flash) {
        Pedido pedido = clienteService.findPedidoById(id);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe en la base de datos!");
            return "redirect:/pedidos/listarPedidos";
        }

        workshopService.removeDuplicateChecklistsForPedido(id);
        
        List<ChecklistInstance> checklists = workshopService.getChecklistsByPedido(id);
        
        log.info("Checklist count for pedido {}: {}", id, checklists.size());
        for (ChecklistInstance instance : checklists) {
            log.info("  - Template: {} (ID: {}), Items count: {}", 
                instance.getTemplate().getCode(), 
                instance.getTemplate().getId(),
                instance.getItems().size());
            for (ChecklistItemInstance item : instance.getItems()) {
                log.info("    > Item ID: {}, Label: {}, Type: {}", item.getId(), item.getLabel(), item.getInputType());
            }
        }
        
        if (checklists.isEmpty()) {
            log.info("No checklists found, creating new ones for pedido {}", id);
            workshopService.instantiateChecklistsForPedido(pedido);
            checklists = workshopService.getChecklistsByPedido(id);
            log.info("After instantiation, checklist count: {}", checklists.size());
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("checklists", checklists);
        model.addAttribute("titulo", "Checklist de Taller");
        
        return "workshop/checklist";
    }
    
    @GetMapping("/checklist/{id}/detail/{code}")
    public String checklistDetail(@PathVariable(value = "id") Long id, 
                                   @PathVariable(value = "code") String code,
                                   Model model, 
                                   RedirectAttributes flash) {
        Pedido pedido = clienteService.findPedidoById(id);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe en la base de datos!");
            return "redirect:/pedidos/listarPedidos";
        }

        ChecklistInstance instance = workshopService.getChecklistInstanceByPedidoAndCode(id, code);
        if (instance == null) {
            flash.addFlashAttribute("error", "El checklist no existe!");
            return "redirect:/workshop/checklist/" + id;
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("instance", instance);
        model.addAttribute("titulo", instance.getTemplate().getName());
        
        return "workshop/checklist-detail";
    }
}

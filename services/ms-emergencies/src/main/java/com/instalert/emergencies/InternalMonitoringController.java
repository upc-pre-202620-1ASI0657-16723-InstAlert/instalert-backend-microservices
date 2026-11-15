package com.instalert.emergencies;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/emergencies/internal/monitoring")
public class InternalMonitoringController {
 private final EmergencyRepository repository;
 public InternalMonitoringController(EmergencyRepository repository){this.repository=repository;}
 @GetMapping("/{accountId}") public Object active(@PathVariable UUID accountId){
  return repository.findAll((r,q,b)->b.and(b.equal(r.get("ownerAccountId"),accountId.toString()),b.equal(r.get("status"),"ACTIVE")),PageRequest.of(0,50)).getContent().stream().map(EmergencyController.AlertResponse::from).toList();
 }
}

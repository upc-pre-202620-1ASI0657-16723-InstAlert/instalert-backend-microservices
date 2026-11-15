package com.instalert_backend.incidents.evidence;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.io.IOException;
@RestController @RequestMapping("/api/v1/incidents/{incidentId}/evidences")
public class EvidenceController {
 private final EvidenceService service;
 public EvidenceController(EvidenceService service){this.service=service;}
 @GetMapping public Object list(@PathVariable long incidentId){return service.list(incidentId);}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 public Object upload(@PathVariable long incidentId,@RequestPart("file") MultipartFile file) throws IOException{return service.upload(incidentId,file);}
 @GetMapping("/{id}") public ResponseEntity<byte[]> download(@PathVariable long incidentId,@PathVariable UUID id) throws IOException{
  var d=service.download(incidentId,id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.type())).header("Content-Disposition","attachment; filename=\"evidence-"+id+"\"").header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(d.bytes());
 }
}

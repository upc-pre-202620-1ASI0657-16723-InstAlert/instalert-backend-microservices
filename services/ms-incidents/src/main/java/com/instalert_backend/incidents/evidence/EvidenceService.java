package com.instalert_backend.incidents.evidence;

import com.instalert_backend.incidents.domain.repositories.IncidentRepository;
import com.instalert_backend.transition.ReferenceAuthorization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.io.IOException;

/** Private local storage: generated object names only, with recoverable upload states. */
@Service
public class EvidenceService {
 private final JdbcTemplate db; private final IncidentRepository incidents; private final Path root;
 public EvidenceService(JdbcTemplate db,IncidentRepository incidents,@Value("${evidence.storage.path}") String path) throws IOException {
  this.db=db;this.incidents=incidents;root=Path.of(path).toAbsolutePath().normalize();Files.createDirectories(root);
 }
 public void authorize(long incident){
  var report=incidents.findById(incident).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  ReferenceAuthorization.requireOwnerOrAdmin(report.getUserId());
 }
 public List<Map<String,Object>> list(long incident){authorize(incident);return db.queryForList("SELECT id,original_name,content_type,byte_size,state FROM incident_evidences WHERE incident_id=? ORDER BY created_at",incident);}
 public Map<String,Object> upload(long incident,MultipartFile file) throws IOException {
  authorize(incident);
  if(file.isEmpty() || file.getSize()>5*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"File must be at most 5 MB");
  byte[] bytes=file.getBytes();String type;
  if(bytes.length>=8 && Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) type="image/png";
  else if(bytes.length>=3 && bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255)type="image/jpeg";
  else if(bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))type="application/pdf";
  else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PNG, JPEG and PDF content is accepted");
  if(!type.equals(file.getContentType()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Content does not match declared type");
  String id=UUID.randomUUID().toString();String name=Optional.ofNullable(file.getOriginalFilename()).orElse("evidence").replaceAll("[\\r\\n\\\\/]","_");
  if(name.length()>180)name=name.substring(name.length()-180);
  db.update("INSERT INTO incident_evidences(id,incident_id,original_name,content_type,byte_size,state,created_at) VALUES(?,?,?,?,?,'PENDING',?)",id,incident,name,type,bytes.length,java.sql.Timestamp.from(Instant.now()));
  try {
   Files.write(root.resolve(id+".part"),bytes,StandardOpenOption.CREATE_NEW);
   Files.move(root.resolve(id+".part"),root.resolve(id),StandardCopyOption.ATOMIC_MOVE);
   db.update("UPDATE incident_evidences SET state='AVAILABLE' WHERE id=?",id);
  }catch(IOException e){db.update("UPDATE incident_evidences SET state='FAILED' WHERE id=?",id);Files.deleteIfExists(root.resolve(id+".part"));Files.deleteIfExists(root.resolve(id));throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Storage unavailable");}
  return db.queryForMap("SELECT id,original_name,content_type,byte_size,state FROM incident_evidences WHERE id=?",id);
 }
 public record Download(byte[] bytes,String type){}
 public Download download(long incident,UUID id) throws IOException {
  authorize(incident);var rows=db.queryForList("SELECT content_type,state FROM incident_evidences WHERE id=? AND incident_id=?",id.toString(),incident);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(!"AVAILABLE".equals(rows.getFirst().get("state")))throw new ResponseStatusException(HttpStatus.CONFLICT);
  Path p=root.resolve(id.toString());if(!Files.isRegularFile(p))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
  return new Download(Files.readAllBytes(p),rows.getFirst().get("content_type").toString());
 }
 @Scheduled(fixedDelay=60000) public void recover(){
  // A process crash between move and DB acknowledgment is recovered from the durable object.
  for(var row:db.queryForList("SELECT id FROM incident_evidences WHERE state='PENDING' AND created_at<?",java.sql.Timestamp.from(Instant.now().minusSeconds(120)))){
   String id=row.get("id").toString();boolean ready=Files.isRegularFile(root.resolve(id));
   db.update("UPDATE incident_evidences SET state=? WHERE id=? AND state='PENDING'",ready?"AVAILABLE":"FAILED",id);
   try{Files.deleteIfExists(root.resolve(id+".part"));}catch(IOException ignored){}
  }
 }
}

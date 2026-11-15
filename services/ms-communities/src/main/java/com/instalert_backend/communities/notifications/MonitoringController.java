package com.instalert_backend.communities.notifications;
import com.instalert_backend.transition.ReferenceAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/communities")
public class MonitoringController {
 private final JdbcTemplate db;private final RestClient emergencies;private final String key;
 public MonitoringController(JdbcTemplate db,@Value("${emergencies.service.url}") String url,@Value("${notifications.internal-key}") String key){
  this.db=db;this.key=key;var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(2000);factory.setReadTimeout(3000);emergencies=RestClient.builder().baseUrl(url).requestFactory(factory).build();
 }
 public record Grant(@NotNull UUID accountId,@NotNull Instant expiresAt,@NotNull @Pattern(regexp="EMERGENCY_STATUS") String scope){}
 private boolean accepted(String owner,String recipient){return db.queryForObject("SELECT COUNT(*) FROM notification_contacts WHERE status='ACCEPTED' AND ((requester=? AND recipient=?) OR (requester=? AND recipient=?))",Integer.class,owner,recipient,recipient,owner)>0;}
 @GetMapping("/monitoring-grants") public Object grants(){var me=ReferenceAuthorization.current().accountId();return db.queryForList("SELECT * FROM monitoring_grants WHERE owner_account_id=? OR recipient_account_id=?",me,me);}
 @PostMapping("/monitoring-grants") @ResponseStatus(HttpStatus.CREATED) public Object grant(@Valid @RequestBody Grant g){
  String me=ReferenceAuthorization.current().accountId(),target=g.accountId().toString();
  if(!accepted(me,target))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(!g.expiresAt().isAfter(Instant.now()) || g.expiresAt().isAfter(Instant.now().plusSeconds(86400)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  String id=UUID.randomUUID().toString();db.update("INSERT INTO monitoring_grants(id,owner_account_id,recipient_account_id,scope,expires_at) VALUES(?,?,?,?,?)",id,me,target,g.scope(),java.sql.Timestamp.from(g.expiresAt()));return Map.of("id",id,"expiresAt",g.expiresAt());
 }
 @DeleteMapping("/monitoring-grants/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void revoke(@PathVariable UUID id){if(db.update("UPDATE monitoring_grants SET revoked=1 WHERE id=? AND owner_account_id=?",id.toString(),ReferenceAuthorization.current().accountId())==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 @GetMapping("/monitoring/{accountId}") public Object monitor(@PathVariable UUID accountId){
  String owner=accountId.toString(),recipient=ReferenceAuthorization.current().accountId();
  if(!accepted(owner,recipient) || db.queryForObject("SELECT COUNT(*) FROM monitoring_grants WHERE owner_account_id=? AND recipient_account_id=? AND revoked=0 AND scope='EMERGENCY_STATUS' AND expires_at>?",Integer.class,owner,recipient,java.sql.Timestamp.from(Instant.now()))==0)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(key.length()<32)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
  try{return emergencies.get().uri("/api/v1/emergencies/internal/monitoring/"+owner).header("X-Notification-Key",key).retrieve().body(Object.class);}
  catch(RestClientException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Emergency state unavailable");}
 }
}

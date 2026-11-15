package com.instalert_backend.communities.interfaces.rest;
import com.instalert_backend.transition.ReferenceAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/communities")
@io.swagger.v3.oas.annotations.tags.Tag(name="Communities")
public class CommunitiesController {
 private final JdbcTemplate db;
 public CommunitiesController(JdbcTemplate db){this.db=db;}
 public record Create(@NotBlank @Size(max=100) String name,@Size(max=255) String description,boolean isPrivate,Long ownerId){}
 public record Support(@NotBlank @Size(max=1000) String description){}
 private Map<String,Object> raw(long id){var rows=db.queryForList("SELECT * FROM communities WHERE id=?",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return rows.getFirst();}
 private boolean owner(Map<String,Object> c){return ((Number)c.get("owner_id")).longValue()==ReferenceAuthorization.current().referenceId() || ReferenceAuthorization.admin();}
 private boolean member(long id){return db.queryForObject("SELECT COUNT(*) FROM community_memberships WHERE community_id=? AND account_id=?",Integer.class,id,ReferenceAuthorization.current().accountId())>0;}
 private void readable(long id){var c=raw(id);if(Boolean.TRUE.equals(c.get("is_private")) && !owner(c) && !member(id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 private void membership(long id){if(!owner(raw(id)) && !member(id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
 private Map<String,Object> dto(Map<String,Object> c){return Map.of("id",c.get("id"),"name",c.get("name"),"description",Objects.toString(c.get("description"),""),"isPrivate",c.get("is_private"),"ownerId",c.get("owner_id"),"member",member(((Number)c.get("id")).longValue()));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public Object create(@Valid @RequestBody Create c){
  var me=ReferenceAuthorization.current();if(c.ownerId()!=null)ReferenceAuthorization.requireOwner(c.ownerId());
  var keys=new org.springframework.jdbc.support.GeneratedKeyHolder();
  Object[] values={c.name(),c.description(),c.isPrivate(),me.referenceId()};
  db.update(connection->{var statement=connection.prepareStatement("INSERT INTO communities(created_at,updated_at,name,description,is_private,owner_id) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?,?,?)",new String[]{"id"});for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},keys);
  long id=keys.getKey().longValue();db.update("INSERT INTO community_memberships(community_id,account_id) VALUES(?,?)",id,me.accountId());return get(id);
 }
 @GetMapping public Object list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){
  if(page<0 || page>10000 || size<1 || size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var me=ReferenceAuthorization.current();return db.queryForList("SELECT * FROM communities c WHERE is_private=false OR owner_id=? OR EXISTS(SELECT 1 FROM community_memberships m WHERE m.community_id=c.id AND m.account_id=?) ORDER BY id DESC LIMIT ? OFFSET ?",me.referenceId(),me.accountId(),size,page*size).stream().map(this::dto).toList();
 }
 @GetMapping("/{id}") public Object get(@PathVariable long id){readable(id);return dto(raw(id));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id){if(!owner(raw(id)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);db.update("DELETE FROM communities WHERE id=?",id);}
 @PostMapping("/{id}/members/me") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void join(@PathVariable long id){
  var c=raw(id);if(Boolean.TRUE.equals(c.get("is_private")) && !owner(c))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(!member(id))db.update("INSERT INTO community_memberships(community_id,account_id) VALUES(?,?)",id,ReferenceAuthorization.current().accountId());
 }
 @DeleteMapping("/{id}/members/me") @ResponseStatus(HttpStatus.NO_CONTENT) public void leave(@PathVariable long id){var c=raw(id);if(owner(c))throw new ResponseStatusException(HttpStatus.CONFLICT);db.update("DELETE FROM community_memberships WHERE community_id=? AND account_id=?",id,ReferenceAuthorization.current().accountId());}
 @PostMapping("/{id}/support-requests") @ResponseStatus(HttpStatus.CREATED) public Object support(@PathVariable long id,@Valid @RequestBody Support body){membership(id);String key=UUID.randomUUID().toString();db.update("INSERT INTO community_support_requests(id,community_id,account_id,description,status,created_at) VALUES(?,?,?,?,'ACTIVE',?)",key,id,ReferenceAuthorization.current().accountId(),body.description(),java.sql.Timestamp.from(Instant.now()));return Map.of("id",key,"status","ACTIVE");}
 @GetMapping("/{id}/support-requests") public Object support(@PathVariable long id,@RequestParam(defaultValue="0") int page){membership(id);if(page<0 || page>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);return db.queryForList("SELECT * FROM community_support_requests WHERE community_id=? ORDER BY created_at DESC LIMIT 50 OFFSET ?",id,page*50);}
 @PutMapping("/{id}/support-requests/{requestId}/resolve") @ResponseStatus(HttpStatus.NO_CONTENT) public void resolve(@PathVariable long id,@PathVariable UUID requestId){membership(id);var rows=db.queryForList("SELECT account_id FROM community_support_requests WHERE id=? AND community_id=?",requestId.toString(),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(!owner(raw(id)) && !rows.getFirst().get("account_id").equals(ReferenceAuthorization.current().accountId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);db.update("UPDATE community_support_requests SET status='RESOLVED' WHERE id=?",requestId.toString());}
}

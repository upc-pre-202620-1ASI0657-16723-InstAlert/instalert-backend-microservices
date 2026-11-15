package com.instalert_backend.incidents.interfaces.rest;
import com.instalert_backend.transition.ReferenceAuthorization;
import com.instalert_backend.incidents.interfaces.rest.resources.IncidentResource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/incidents")
@io.swagger.v3.oas.annotations.tags.Tag(name="Incidents",description="Authenticated report map and owner-managed reports")
public class IncidentsController {
 private final JdbcTemplate db;
 public IncidentsController(JdbcTemplate db){this.db=db;}
 public record Create(Long userId,@NotBlank @Size(max=100) String type,@Pattern(regexp="HIGH|MEDIUM|LOW|CRITICAL") String severity,
  @NotBlank @Size(max=255) String address,@NotBlank @Size(max=255) String description,@Pattern(regexp="ACTIVE") String status,
  @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,@NotBlank String timeReported){}
 public record State(@NotNull @Pattern(regexp="ACTIVE|RESOLVED|CLOSED") String status){}
 private static final RowMapper<IncidentResource> MAPPER=(r,n)->new IncidentResource(r.getLong("id"),r.getLong("user_id"),r.getString("type"),r.getString("severity"),r.getString("address"),r.getString("description"),r.getString("status"),r.getDouble("latitude"),r.getDouble("longitude"),r.getString("time_reported"));
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public IncidentResource create(@Valid @RequestBody Create c){
  var me=ReferenceAuthorization.current();if(c.userId()!=null)ReferenceAuthorization.requireOwner(c.userId());
  try{if(Instant.parse(c.timeReported()).isAfter(Instant.now().plusSeconds(60)))throw new IllegalArgumentException();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid report date");}
  var keys=new org.springframework.jdbc.support.GeneratedKeyHolder();
  Object[] values={me.referenceId(),c.type(),c.severity()==null?"MEDIUM":c.severity(),c.address(),c.description(),c.latitude(),c.longitude(),c.timeReported()};
  db.update(connection->{var statement=connection.prepareStatement("INSERT INTO incidents(created_at,updated_at,user_id,type,severity,address,description,status,latitude,longitude,time_reported) VALUES(CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?,?,?,?,'ACTIVE',?,?,?)",new String[]{"id"});for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},keys);
  return get(keys.getKey().longValue());
 }
 @GetMapping("/{id}") public IncidentResource get(@PathVariable long id){
  var rows=db.query("SELECT * FROM incidents WHERE id=? AND status<>'DELETED'",MAPPER,id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return rows.getFirst();
 }
 @GetMapping public Object query(@RequestParam(required=false) String type,@RequestParam(required=false) String severity,@RequestParam(required=false) String status,
  @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,
  @RequestParam(required=false) Double minLat,@RequestParam(required=false) Double maxLat,@RequestParam(required=false) Double minLng,@RequestParam(required=false) Double maxLng,
  @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size,@RequestParam(defaultValue="false") boolean paged){
  if(page<0 || page>10000 || size<1 || size>100 || (from!=null && to!=null && from.isAfter(to)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  var sql=new StringBuilder(" FROM incidents WHERE status<>'DELETED'");var args=new ArrayList<Object>();
  for(var pair:new String[][]{{"type",type},{"severity",severity},{"status",status}})if(pair[1]!=null){sql.append(" AND ").append(pair[0]).append("=?");args.add(pair[1]);}
  if(from!=null){sql.append(" AND time_reported>=?");args.add(from.toString());}if(to!=null){sql.append(" AND time_reported<=?");args.add(to.toString());}
  var bounds=Arrays.asList(minLat,maxLat,minLng,maxLng);
  if(bounds.stream().anyMatch(Objects::nonNull)){
   if(bounds.stream().anyMatch(Objects::isNull) || bounds.stream().anyMatch(x->!Double.isFinite(x)) || minLat < -90 || maxLat>90 || minLng < -180 || maxLng>180 || minLat>maxLat || minLng>maxLng)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
   sql.append(" AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?");args.addAll(bounds);
  }
  long total=db.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());args.add(size);args.add(page*size);
  var rows=db.query("SELECT *"+sql+" ORDER BY id DESC LIMIT ? OFFSET ?",MAPPER,args.toArray());
  return paged?Map.of("content",rows,"page",page,"size",size,"totalElements",total,"totalPages",(total+size-1)/size):rows;
 }
 @PutMapping("/{id}/status") public IncidentResource update(@PathVariable long id,@Valid @RequestBody State s){
  var r=get(id);ReferenceAuthorization.requireOwnerOrAdmin(r.userId());db.update("UPDATE incidents SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",s.status(),id);return get(id);
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long id){
  var r=get(id);ReferenceAuthorization.requireOwnerOrAdmin(r.userId());db.update("UPDATE incidents SET status='DELETED',updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
 }
}

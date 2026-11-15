package com.instalert_backend;
import com.instalert_backend.incidents.evidence.EvidenceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:evidence_recovery;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password="})
class EvidenceRecoveryTest {
 static final Path root;
 static {try{root=Files.createTempDirectory("instalert-recovery-");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 @DynamicPropertySource static void storage(DynamicPropertyRegistry r){r.add("evidence.storage.path",root::toString);}
 @Autowired EvidenceService service; @Autowired JdbcTemplate db;
 @BeforeEach void reset(){db.update("DELETE FROM incident_evidences");db.update("DELETE FROM incidents");db.update("INSERT INTO incidents(id,created_at,updated_at,user_id,status) VALUES(1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,'ACTIVE')");}
 String pending(long age){String id=UUID.randomUUID().toString();db.update("INSERT INTO incident_evidences(id,incident_id,original_name,content_type,byte_size,state,created_at) VALUES(?,1,'sample.pdf','application/pdf',10,'PENDING',?)",id,java.sql.Timestamp.from(Instant.now().minusSeconds(age)));return id;}
 String state(String id){return db.queryForObject("SELECT state FROM incident_evidences WHERE id=?",String.class,id);}
 @Test void recoversDurableFileAfterDatabaseAcknowledgmentWasInterrupted() throws Exception {String id=pending(300);byte[] bytes="%PDF-test".getBytes();Files.write(root.resolve(id),bytes);service.recover();assertThat(state(id)).isEqualTo("AVAILABLE");assertThat(Files.readAllBytes(root.resolve(id))).isEqualTo(bytes);service.recover();assertThat(state(id)).isEqualTo("AVAILABLE");}
 @Test void failsAndRemovesIncompletePartWithoutPublishingIt() throws Exception {String id=pending(300);Files.writeString(root.resolve(id+".part"),"incomplete");service.recover();assertThat(state(id)).isEqualTo("FAILED");assertThat(root.resolve(id+".part")).doesNotExist();assertThat(root.resolve(id)).doesNotExist();}
 @Test void failsMissingObjectRatherThanClaimingItAvailable(){String id=pending(300);service.recover();assertThat(state(id)).isEqualTo("FAILED");}
 @Test void leavesFreshUploadUntouched() throws Exception {String id=pending(0);Files.writeString(root.resolve(id+".part"),"uploading");service.recover();assertThat(state(id)).isEqualTo("PENDING");assertThat(root.resolve(id+".part")).exists();}
 @AfterAll static void cleanup() throws Exception {try(var paths=Files.list(root)){for(Path p:paths.toList())Files.delete(p);}Files.delete(root);}
}

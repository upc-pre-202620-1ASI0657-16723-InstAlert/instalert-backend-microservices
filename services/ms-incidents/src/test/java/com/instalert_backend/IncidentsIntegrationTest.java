package com.instalert_backend;
import com.instalert_backend.transition.AccountSecurityConfiguration.AccountPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:incidents;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","evidence.storage.path=target/test-evidence","notifications.internal-key=01234567890123456789012345678901"})
@AutoConfigureMockMvc
class IncidentsIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 static final String A="11111111-1111-4111-8111-111111111111",B="22222222-2222-4222-8222-222222222222";
 org.springframework.test.web.servlet.request.RequestPostProcessor me(String id,long ref){return authentication(new UsernamePasswordAuthenticationToken(new AccountPrincipal(id,ref,List.of("ROLE_USER")),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));}

 long create() throws Exception {return json.readTree(mvc.perform(post("/api/v1/incidents").with(me(A,1)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("type","TEST","severity","HIGH","address","Local","description","Synthetic test","latitude",-12.0,"longitude",-77.0,"timeReported",Instant.now().toString())))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();}
 @Test void requiresAuthentication()throws Exception{mvc.perform(get("/api/v1/incidents")).andExpect(status().isUnauthorized());}
 @Test void createsOwnerAndPaginates()throws Exception{long id=create();mvc.perform(get("/api/v1/incidents").param("paged","true").param("type","TEST").param("size","1").with(me(A,1))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].userId").value(1)).andExpect(jsonPath("$.size").value(1));}
 @Test void deniesOtherOwnerAndKeepsState()throws Exception{long id=create();mvc.perform(put("/api/v1/incidents/"+id+"/status").with(me(B,2)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}")).andExpect(status().isForbidden());mvc.perform(get("/api/v1/incidents/"+id).with(me(A,1))).andExpect(jsonPath("$.status").value("ACTIVE"));}
 @Test void rejectsInvalidCoordinatesAndSpoofedOwner()throws Exception{mvc.perform(post("/api/v1/incidents").with(me(A,1)).contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":999}")).andExpect(status().isBadRequest());mvc.perform(get("/api/v1/incidents").param("size","999").with(me(A,1))).andExpect(status().isBadRequest());}
 @Test void evidenceIsPrivateAndSignatureValidated()throws Exception{long id=create();var file=new MockMultipartFile("file","sample.pdf","application/pdf","%PDF-1.4 synthetic".getBytes());var r=mvc.perform(multipart("/api/v1/incidents/"+id+"/evidences").file(file).with(me(A,1))).andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("AVAILABLE")).andReturn();String key=json.readTree(r.getResponse().getContentAsString()).path("id").asText();mvc.perform(get("/api/v1/incidents/"+id+"/evidences/"+key).with(me(B,2))).andExpect(status().isForbidden());mvc.perform(get("/api/v1/incidents/"+id+"/evidences/"+key).with(me(A,1))).andExpect(status().isOk()).andExpect(content().bytes(file.getBytes()));mvc.perform(multipart("/api/v1/incidents/"+id+"/evidences").file(new MockMultipartFile("file","fake.png","image/png","html".getBytes())).with(me(A,1))).andExpect(status().isBadRequest());}
 @Test void softDeleteKeepsEvidenceReferencesAndHidesReport()throws Exception{long id=create();mvc.perform(delete("/api/v1/incidents/"+id).with(me(A,1))).andExpect(status().isNoContent());mvc.perform(get("/api/v1/incidents/"+id).with(me(A,1))).andExpect(status().isNotFound());}
}

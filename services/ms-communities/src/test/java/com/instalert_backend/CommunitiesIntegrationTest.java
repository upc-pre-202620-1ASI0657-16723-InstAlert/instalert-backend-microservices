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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:communities;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","evidence.storage.path=target/test-evidence","notifications.internal-key=01234567890123456789012345678901"})
@AutoConfigureMockMvc
class CommunitiesIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 static final String A="11111111-1111-4111-8111-111111111111",B="22222222-2222-4222-8222-222222222222";
 org.springframework.test.web.servlet.request.RequestPostProcessor me(String id,long ref){return authentication(new UsernamePasswordAuthenticationToken(new AccountPrincipal(id,ref,List.of("ROLE_USER")),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));}

 long create(boolean hidden)throws Exception{return json.readTree(mvc.perform(post("/api/v1/communities").with(me(A,1)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Synthetic group","description","Local test","isPrivate",hidden)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();}
 @Test void requiresAuthentication()throws Exception{mvc.perform(get("/api/v1/communities")).andExpect(status().isUnauthorized());}
 @Test void privateCommunityNotVisibleToStranger()throws Exception{long id=create(true);mvc.perform(get("/api/v1/communities/"+id).with(me(B,2))).andExpect(status().isForbidden());mvc.perform(delete("/api/v1/communities/"+id).with(me(B,2))).andExpect(status().isForbidden());}
 @Test void publicMembershipAndSupportPersist()throws Exception{long id=create(false);mvc.perform(post("/api/v1/communities/"+id+"/support-requests").with(me(B,2)).contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Need help\"}")).andExpect(status().isForbidden());mvc.perform(post("/api/v1/communities/"+id+"/members/me").with(me(B,2))).andExpect(status().isNoContent());mvc.perform(post("/api/v1/communities/"+id+"/support-requests").with(me(B,2)).contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Need help\"}")).andExpect(status().isCreated());mvc.perform(get("/api/v1/communities/"+id+"/support-requests").with(me(A,1))).andExpect(jsonPath("$[0].description").value("Need help"));}
 @Test void monitorRequiresExplicitConsent()throws Exception{mvc.perform(get("/api/v1/communities/monitoring/"+B).with(me(A,1))).andExpect(status().isForbidden());}
 @Test void invitedUserAloneAcceptsAndGrantCanBeRevoked()throws Exception{String id=json.readTree(mvc.perform(post("/api/v1/communities/contacts").with(me(A,1)).contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":\""+B+"\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();mvc.perform(put("/api/v1/communities/contacts/"+id+"/accept").with(me(A,1))).andExpect(status().isForbidden());mvc.perform(put("/api/v1/communities/contacts/"+id+"/accept").with(me(B,2))).andExpect(status().isNoContent());String grant=json.readTree(mvc.perform(post("/api/v1/communities/monitoring-grants").with(me(A,1)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("accountId",B,"scope","EMERGENCY_STATUS","expiresAt",Instant.now().plusSeconds(3600).toString())))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();mvc.perform(delete("/api/v1/communities/monitoring-grants/"+grant).with(me(B,2))).andExpect(status().isNotFound());mvc.perform(delete("/api/v1/communities/monitoring-grants/"+grant).with(me(A,1))).andExpect(status().isNoContent());mvc.perform(get("/api/v1/communities/monitoring/"+A).with(me(B,2))).andExpect(status().isForbidden());}
 @Test void internalIngestionRejectsBrowserSession()throws Exception{mvc.perform(post("/api/v1/communities/internal/emergency-events").with(me(A,1)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());}
}

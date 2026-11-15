package com.instalert_backend.bdd;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instalert_backend.transition.AccountSecurityConfiguration.AccountPrincipal;
import io.cucumber.java.en.*;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.*;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
@CucumberContextConfiguration
@SpringBootTest(classes=com.instalert_backend.IncidentsApplication.class,properties={"spring.datasource.url=jdbc:h2:mem:incidents_bdd;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","evidence.storage.path=target/bdd-evidence"})
@AutoConfigureMockMvc
public class IncidentsSteps {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 static final String A="11111111-1111-4111-8111-111111111111", B="22222222-2222-4222-8222-222222222222";
 MvcResult response; long id;
 org.springframework.test.web.servlet.request.RequestPostProcessor me(String account,long reference){return authentication(new UsernamePasswordAuthenticationToken(new AccountPrincipal(account,reference,List.of("ROLE_USER")),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));}
 String evidenceId;
 @When("a citizen submits a valid incident") public void create() throws Exception {
  response=mvc.perform(post("/api/v1/incidents").with(me(A,1)).contentType("application/json").content(json.writeValueAsString(Map.of("type","BDD","address","Synthetic location","description","Acceptance fixture","latitude",-12.0,"longitude",-77.0,"timeReported",Instant.now().toString())))).andReturn();
  assertThat(response.getResponse().getStatus()).isEqualTo(201); id=json.readTree(response.getResponse().getContentAsString()).path("id").asLong();
 }
 @Then("the incident is active and owned by that citizen") public void owner() throws Exception {var body=json.readTree(response.getResponse().getContentAsString());assertThat(body.path("status").asText()).isEqualTo("ACTIVE");assertThat(body.path("userId").asLong()).isEqualTo(1);}
 @Given("a citizen incident") public void incident() throws Exception {create();}
 @When("another citizen resolves that incident") public void other() throws Exception {response=mvc.perform(put("/api/v1/incidents/"+id+"/status").with(me(B,2)).contentType("application/json").content("{\"status\":\"RESOLVED\"}")).andReturn();}
 @Then("the incident operation returns {int}") public void status(int code){assertThat(response.getResponse().getStatus()).isEqualTo(code);}
 @Then("the incident remains active") public void unchanged() throws Exception {var r=mvc.perform(get("/api/v1/incidents/"+id).with(me(A,1))).andReturn();assertThat(json.readTree(r.getResponse().getContentAsString()).path("status").asText()).isEqualTo("ACTIVE");}
 @Given("a citizen incident with private evidence") public void file() throws Exception {create();var f=new org.springframework.mock.web.MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.4 synthetic".getBytes());var r=mvc.perform(multipart("/api/v1/incidents/"+id+"/evidences").file(f).with(me(A,1))).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(201);evidenceId=json.readTree(r.getResponse().getContentAsString()).path("id").asText();}
 @When("another citizen downloads the evidence") public void download() throws Exception {response=mvc.perform(get("/api/v1/incidents/"+id+"/evidences/"+evidenceId).with(me(B,2))).andReturn();}
}

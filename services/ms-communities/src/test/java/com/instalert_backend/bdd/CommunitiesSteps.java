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
@SpringBootTest(classes=com.instalert_backend.CommunitiesApplication.class,properties={"spring.datasource.url=jdbc:h2:mem:communities_bdd;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","notifications.internal-key=01234567890123456789012345678901"})
@AutoConfigureMockMvc
public class CommunitiesSteps {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 static final String A="11111111-1111-4111-8111-111111111111", B="22222222-2222-4222-8222-222222222222";
 MvcResult response; long id;
 org.springframework.test.web.servlet.request.RequestPostProcessor me(String account,long reference){return authentication(new UsernamePasswordAuthenticationToken(new AccountPrincipal(account,reference,List.of("ROLE_USER")),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));}
 String link,grant;
 @Given("a private community owned by a citizen") public void community() throws Exception {response=mvc.perform(post("/api/v1/communities").with(me(A,1)).contentType("application/json").content("{\"name\":\"BDD private\",\"isPrivate\":true}")).andReturn();assertThat(response.getResponse().getStatus()).isEqualTo(201);id=json.readTree(response.getResponse().getContentAsString()).path("id").asLong();}
 @When("another citizen reads the private community") public void read() throws Exception {response=mvc.perform(get("/api/v1/communities/"+id).with(me(B,2))).andReturn();}
 @Then("the community operation returns {int}") public void status(int code){assertThat(response.getResponse().getStatus()).isEqualTo(code);}
 @Given("a pending trust invitation between two citizens") public void invitation() throws Exception {
  // Distinct citizens per scenario avoid coupling through unique contact pairs.
  first=UUID.randomUUID().toString();second=UUID.randomUUID().toString();
  response=mvc.perform(post("/api/v1/communities/contacts").with(me(first,11)).contentType("application/json").content(json.writeValueAsString(Map.of("accountId",second)))).andReturn();assertThat(response.getResponse().getStatus()).isEqualTo(201);link=json.readTree(response.getResponse().getContentAsString()).path("id").asText();
 }
 String first,second;
 @When("the requester accepts their own invitation") public void wrongAccept() throws Exception {response=mvc.perform(put("/api/v1/communities/contacts/"+link+"/accept").with(me(first,11))).andReturn();}
 @When("the recipient accepts the invitation") public void accept() throws Exception {response=mvc.perform(put("/api/v1/communities/contacts/"+link+"/accept").with(me(second,12))).andReturn();}
 @Given("an accepted trust link with monitoring permission") public void permission() throws Exception {invitation();accept();assertThat(response.getResponse().getStatus()).isEqualTo(204);response=mvc.perform(post("/api/v1/communities/monitoring-grants").with(me(first,11)).contentType("application/json").content(json.writeValueAsString(Map.of("accountId",second,"scope","EMERGENCY_STATUS","expiresAt",Instant.now().plusSeconds(3600).toString())))).andReturn();assertThat(response.getResponse().getStatus()).isEqualTo(201);grant=json.readTree(response.getResponse().getContentAsString()).path("id").asText();}
 @When("the owner revokes monitoring permission") public void revoke() throws Exception {response=mvc.perform(delete("/api/v1/communities/monitoring-grants/"+grant).with(me(first,11))).andReturn();}
 @When("the recipient queries monitoring after revocation") public void monitor() throws Exception {response=mvc.perform(get("/api/v1/communities/monitoring/"+first).with(me(second,12))).andReturn();}
}

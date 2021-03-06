package it.infn.mw.iam.test.oauth;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import it.infn.mw.iam.IamLoginService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@Transactional
public class ResourceOwnerPasswordCredentialsTests {

  private static final String GRANT_TYPE = "password";
  private static final String USERNAME = "test";
  private static final String PASSWORD = "password";
  private static final String SCOPE = "openid profile";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mvc;

  @Before
  public void setup() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(print())
      .build();
  }

  @Test
  public void testDiscoveryEndpoint() throws Exception {

    // @formatter:off
    mvc.perform(get("/.well-known/openid-configuration"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.issuer", equalTo("http://localhost:8080/")));
    // @formatter:on
  }

  @Test
  public void testResourceOwnerPasswordCredentialsFlow() throws Exception {

    String clientId = "password-grant";
    String clientSecret = "secret";

    // @formatter:off
    mvc.perform(post("/token")
        .with(httpBasic(clientId, clientSecret))
        .param("grant_type", GRANT_TYPE)
        .param("username", USERNAME)
        .param("password", PASSWORD)
        .param("scope", SCOPE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.scope", equalTo(SCOPE)));
    // @formatter:on
  }

  @Test
  public void testInvalidResourceOwnerPasswordCredentials() throws Exception {

    String clientId = "password-grant";
    String clientSecret = "secret";

    // @formatter:off
    mvc.perform(post("/token")
        .with(httpBasic(clientId, clientSecret))
        .param("grant_type", GRANT_TYPE)
        .param("username", USERNAME)
        .param("password", "wrong_password")
        .param("scope", SCOPE))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error", equalTo("invalid_grant")))
      .andExpect(jsonPath("$.error_description", equalTo("Bad credentials")));
    // @formatter:on
  }

  @Test
  public void testResourceOwnerPasswordCredentialsInvalidClientCredentials() throws Exception {

    String clientId = "password-grant";
    String clientSecret = "socret";

    // @formatter:off
    mvc.perform(post("/token")
        .with(httpBasic(clientId, clientSecret))
        .param("grant_type", GRANT_TYPE)
        .param("username", USERNAME)
        .param("password", PASSWORD)
        .param("scope", SCOPE))
      .andExpect(status().isUnauthorized())
//      .andExpect(jsonPath("$.error", equalTo("Unauthorized")))
//      .andExpect(jsonPath("$.message", equalTo("Bad credentials")))
      ;
    // @formatter:on
  }

  @Test
  public void testResourceOwnerPasswordCredentialsUnknownClient() throws Exception {

    String clientId = "unknown";
    String clientSecret = "socret";

    // @formatter:off
    mvc.perform(post("/token")
        .with(httpBasic(clientId, clientSecret))
        .param("grant_type", GRANT_TYPE)
        .param("username", USERNAME)
        .param("password", PASSWORD)
        .param("scope", SCOPE)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
      .andExpect(status().isUnauthorized())
//      .andExpect(jsonPath("$.error", equalTo("Unauthorized")))
//      .andExpect(jsonPath("$.message", equalTo("Client with id unknown was not found")))
      ;
    // @formatter:on
  }

  @Test
  public void testResourceOwnerPasswordCredentialAuthenticationTimestamp() throws Exception {

    String clientId = "password-grant";
    String clientSecret = "secret";

    // @formatter:off
    String response = mvc.perform(post("/token")
        .with(httpBasic(clientId, clientSecret))
        .param("grant_type", GRANT_TYPE)
        .param("username", USERNAME)
        .param("password", PASSWORD)
        .param("scope", SCOPE))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    // @formatter:on

    DefaultOAuth2AccessToken tokenResponse =
        mapper.readValue(response, DefaultOAuth2AccessToken.class);

    String idToken = tokenResponse.getAdditionalInformation().get("id_token").toString();

    JWT token = JWTParser.parse(idToken);
    System.out.println(token.getJWTClaimsSet());
    assertNotNull(token.getJWTClaimsSet().getClaim("auth_time"));
  }
}

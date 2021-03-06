package it.infn.mw.iam.test.scim.group;

import static it.infn.mw.iam.api.scim.model.ScimConstants.SCIM_CONTENT_TYPE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimListResponse;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimUtils;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
@Transactional
@WithMockOAuthUser(clientId = "scim-client-rw", scopes = {"scim:read"})
public class ScimGroupProvisioningListTests {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  private final static String GROUP_URI = ScimUtils.getGroupsLocation();

  private Integer totalResults = 22;
  private Integer pageSize = 10;

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(print())
      .build();
  }

  @Test
  public void testNoParameterListRequest() throws Exception {

    mvc.perform(get(GROUP_URI).contentType(SCIM_CONTENT_TYPE))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(pageSize)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(pageSize))));
  }

  @Test
  public void testCountAs8Returns8Items() throws Exception {
    Integer count = 8;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(count)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(count))));
    //@formatter:on
  }

  @Test
  public void testCount1Returns1Item() throws Exception {
    Integer count = 1;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(count)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(count))));
    //@formatter:on
  }

  @Test
  public void testCountShouldBeLimitedToTen() throws Exception {
    Integer count = 30;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(pageSize)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(pageSize))));
    //@formatter:on
  }

  @Test
  public void testNegativeCountBecomesZero() throws Exception {
    Integer count = -10;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(0)))
      .andExpect(jsonPath("$.startIndex", equalTo(1)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(0))));
    //@formatter:on
  }

  @Test
  public void testInvalidStartIndex() throws Exception {
    Integer startIndex = 23;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("startIndex", startIndex.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(0)))
      .andExpect(jsonPath("$.startIndex", equalTo(startIndex)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(0))));
    //@formatter:on
  }

  @Test
  public void testRightEndPagination() throws Exception {
    Integer count = 10;
    Integer startIndex = 17;
    Integer items = totalResults - startIndex + 1;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString())
        .param("startIndex", startIndex.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(items)))
      .andExpect(jsonPath("$.startIndex", equalTo(startIndex)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(items))));
    //@formatter:on
  }

  @Test
  public void testLastElementPagination() throws Exception {
    Integer count = 2;
    Integer startIndex = 22;
    Integer items = totalResults - startIndex + 1;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString())
        .param("startIndex", startIndex.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(items)))
      .andExpect(jsonPath("$.startIndex", equalTo(startIndex)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(items))));
    //@formatter:on
  }

  @Test
  public void testFirstElementPagination() throws Exception {
    Integer count = 5;
    Integer startIndex = 1;

    //@formatter:off
    mvc.perform(get(GROUP_URI)
        .contentType(SCIM_CONTENT_TYPE)
        .param("count", count.toString())
        .param("startIndex", startIndex.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalResults", equalTo(totalResults)))
      .andExpect(jsonPath("$.itemsPerPage", equalTo(count)))
      .andExpect(jsonPath("$.startIndex", equalTo(startIndex)))
      .andExpect(jsonPath("$.schemas", contains(ScimListResponse.SCHEMA)))
      .andExpect(jsonPath("$.Resources", hasSize(equalTo(count))));
    //@formatter:on
  }
}

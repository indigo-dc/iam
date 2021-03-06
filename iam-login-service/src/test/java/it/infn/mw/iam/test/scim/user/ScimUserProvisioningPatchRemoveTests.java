package it.infn.mw.iam.test.scim.user;

import static it.infn.mw.iam.api.scim.model.ScimPatchOperation.ScimPatchOperationType.remove;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_CLIENT_ID;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_READ_SCOPE;
import static it.infn.mw.iam.test.scim.ScimUtils.SCIM_WRITE_SCOPE;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.scim.model.ScimOidcId;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.scim.ScimRestUtilsMvc;
import it.infn.mw.iam.test.util.WithMockOAuthUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
    classes = {IamLoginService.class, CoreControllerTestSupport.class, ScimRestUtilsMvc.class})
@WebAppConfiguration
@Transactional
@WithMockOAuthUser(clientId = SCIM_CLIENT_ID, scopes = {SCIM_READ_SCOPE, SCIM_WRITE_SCOPE})
public class ScimUserProvisioningPatchRemoveTests extends ScimUserTestSupport {

  @Autowired
  private ScimRestUtilsMvc scimUtils;

  private List<ScimUser> testUsers = new ArrayList<ScimUser>();

  @Before
  public void setup() throws Exception {

    testUsers = createTestUsers();
  }

  @Test
  public void testPatchRemoveOidcId() throws Exception {

    ScimUser user = testUsers.get(0);

    ScimUser updates =
        ScimUser.builder().addOidcId(user.getIndigoUser().getOidcIds().get(0)).build();

    scimUtils.patchUser(user.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user.getId());

    assertThat(updatedUser.getIndigoUser().getOidcIds(), hasSize(equalTo(0)));
  }

  @Test
  public void testPatchRemoveAnotherUserOidcId() throws Exception {

    ScimUser user1 = testUsers.get(0);
    ScimUser user2 = testUsers.get(1);

    ScimUser updates =
        ScimUser.builder().addOidcId(user2.getIndigoUser().getOidcIds().get(0)).build();

    scimUtils.patchUser(user1.getId(), remove, updates);

    ScimUser updatedUser1 = scimUtils.getUser(user1.getId());
    assertThat(updatedUser1.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));

    ScimUser updatedUser2 = scimUtils.getUser(user2.getId());
    assertThat(updatedUser2.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
  }

  @Test
  public void testPatchRemoveNotFoundOidcId() throws Exception {

    ScimUser user = testUsers.get(0);

    ScimUser updates = ScimUser.builder()
      .addOidcId(ScimOidcId.builder().issuer("fake_issuer").subject("fake_subject").build())
      .build();

    scimUtils.patchUser(user.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user.getId());
    assertThat(updatedUser.getId(), equalTo(user.getId()));
    assertThat(updatedUser.getIndigoUser().getOidcIds(), hasSize(equalTo(1)));
  }

  @Test
  public void testPatchRemoveX509Certificate() throws Exception {

    ScimUser user = testUsers.get(0);

    ScimUser updates = ScimUser.builder()
      .addX509Certificate(user.getIndigoUser().getCertificates().get(0))
      .build();

    scimUtils.patchUser(user.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user.getId());
    assertThat(updatedUser.getId(), equalTo(user.getId()));
    assertThat(updatedUser.getIndigoUser().getCertificates(), empty());
  }

  @Test
  public void testPatchRemoveAnotherUserX509Certificate() throws Exception {

    ScimUser user1 = testUsers.get(0);
    ScimUser user2 = testUsers.get(1);

    ScimUser updates = ScimUser.builder()
      .addX509Certificate(user2.getIndigoUser().getCertificates().get(0))
      .build();

    scimUtils.patchUser(user1.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user1.getId());
    assertThat(updatedUser.getId(), equalTo(user1.getId()));
    assertThat(updatedUser.getIndigoUser().getCertificates(), hasSize(equalTo(1)));
  }

  @Test
  public void testPatchRemoveNotFoundX509Certificate() throws Exception {

    ScimUser user1 = testUsers.get(0);

    ScimUser updates = ScimUser.builder()
      .addX509Certificate(user1.getIndigoUser().getCertificates().get(0))
      .build();

    scimUtils.patchUser(user1.getId(), remove, updates);
    scimUtils.patchUser(user1.getId(), remove, updates);
  }

  @Test
  public void testPatchRemoveSshKey() throws Exception {

    ScimUser user = testUsers.get(0);

    ScimUser updates =
        ScimUser.builder().addSshKey(user.getIndigoUser().getSshKeys().get(0)).build();

    scimUtils.patchUser(user.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user.getId());
    assertThat(updatedUser.getId(), equalTo(user.getId()));
    assertThat(updatedUser.getIndigoUser().getSshKeys(), hasSize(equalTo(0)));
  }

  @Test
  public void testPatchRemoveAnotherUserSshKey() throws Exception {

    ScimUser user1 = testUsers.get(0);
    ScimUser user2 = testUsers.get(1);

    ScimUser updates =
        ScimUser.builder().addSshKey(user2.getIndigoUser().getSshKeys().get(0)).build();

    scimUtils.patchUser(user1.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user1.getId());
    assertThat(updatedUser.getId(), equalTo(user1.getId()));
    assertThat(updatedUser.getIndigoUser().getSshKeys(), hasSize(equalTo(1)));
  }

  @Test
  public void testPatchRemoveNotFoundSshKey() throws Exception {

    ScimUser user1 = testUsers.get(0);

    ScimUser updates =
        ScimUser.builder().addSshKey(user1.getIndigoUser().getSshKeys().get(0)).build();

    scimUtils.patchUser(user1.getId(), remove, updates);
    scimUtils.patchUser(user1.getId(), remove, updates);
  }

  @Test
  public void testPatchRemoveSamlId() throws Exception {

    ScimUser user = testUsers.get(0);

    ScimUser updates =
        ScimUser.builder().addSamlId(user.getIndigoUser().getSamlIds().get(0)).build();

    scimUtils.patchUser(user.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user.getId());
    assertThat(updatedUser.getId(), equalTo(user.getId()));
    assertThat(updatedUser.getIndigoUser().getSamlIds(), hasSize(equalTo(0)));
  }

  @Test
  public void testPatchRemoveAnotherUserSamlId() throws Exception {

    ScimUser user1 = testUsers.get(0);
    ScimUser user2 = testUsers.get(1);

    ScimUser updates =
        ScimUser.builder().addSamlId(user2.getIndigoUser().getSamlIds().get(0)).build();

    scimUtils.patchUser(user1.getId(), remove, updates);

    ScimUser updatedUser1 = scimUtils.getUser(user1.getId());
    assertThat(updatedUser1.getId(), equalTo(user1.getId()));
    assertThat(updatedUser1.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));

    ScimUser updatedUser2 = scimUtils.getUser(user2.getId());
    assertThat(updatedUser2.getId(), equalTo(user2.getId()));
    assertThat(updatedUser2.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));

  }

  @Test
  public void testPatchRemoveNotFoundSamlId() throws Exception {

    ScimUser user = testUsers.get(0);

    ScimUser updates = ScimUser.builder().buildSamlId("fake_idpid", "fake_userid").build();

    scimUtils.patchUser(user.getId(), remove, updates);

    ScimUser updatedUser = scimUtils.getUser(user.getId());
    assertThat(updatedUser.getId(), equalTo(user.getId()));
    assertThat(updatedUser.getIndigoUser().getSamlIds(), hasSize(equalTo(1)));
  }
}

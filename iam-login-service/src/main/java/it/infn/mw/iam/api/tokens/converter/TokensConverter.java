package it.infn.mw.iam.api.tokens.converter;

import it.infn.mw.iam.api.scim.converter.ScimResourceLocationProvider;
import it.infn.mw.iam.api.tokens.TokensResourceLocationProvider;
import it.infn.mw.iam.api.tokens.model.AccessToken;
import it.infn.mw.iam.api.tokens.model.ClientRef;
import it.infn.mw.iam.api.tokens.model.IdTokenRef;
import it.infn.mw.iam.api.tokens.model.RefreshToken;
import it.infn.mw.iam.api.tokens.model.UserRef;
import it.infn.mw.iam.core.user.exception.IamAccountException;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;

import org.mitre.oauth2.model.AuthenticationHolderEntity;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.model.OAuth2RefreshTokenEntity;
import org.mitre.oauth2.model.SavedUserAuthentication;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TokensConverter {

  @Autowired
  private ClientDetailsEntityService clientDetailsService;

  @Autowired
  private IamAccountRepository accountRepository;

  @Autowired
  private TokensResourceLocationProvider tokensResourceLocationProvider;

  @Autowired
  private ScimResourceLocationProvider scimResourceLocationProvider;

  public AccessToken toAccessToken(OAuth2AccessTokenEntity at) {

    AuthenticationHolderEntity ah = at.getAuthenticationHolder();

    ClientRef clientRef = buildClientRef(ah.getClientId());
    UserRef userRef = buildUserRef(ah.getUserAuth());
    IdTokenRef idTokenRef = buildIdTokenRef(at.getIdToken());

    return AccessToken.builder()
        .id(at.getId())
        .client(clientRef)
        .expiration(at.getExpiration())
        .idToken(idTokenRef)
        .scopes(at.getScope())
        .user(userRef)
        .value(at.getValue())
        .build();
  }

  public RefreshToken toRefreshToken(OAuth2RefreshTokenEntity rt) {

    AuthenticationHolderEntity ah = rt.getAuthenticationHolder();

    ClientRef clientRef = buildClientRef(ah.getClientId());
    UserRef userRef = buildUserRef(ah.getUserAuth());

    return RefreshToken.builder()
        .id(rt.getId())
        .client(clientRef)
        .expiration(rt.getExpiration())
        .user(userRef)
        .value(rt.getValue())
        .build();
  }


  private ClientRef buildClientRef(String clientId) {

    if (clientId == null) {
      return null;
    }

    ClientDetailsEntity cd = clientDetailsService.loadClientByClientId(clientId);

    return ClientRef.builder()
        .id(cd.getId())
        .clientId(cd.getClientId())
        .contacts(cd.getContacts())
        .ref(cd.getClientUri())
        .build();
  }

  private UserRef buildUserRef(SavedUserAuthentication userAuth) {

    if (userAuth == null) {
      return null;
    }

    String username = userAuth.getPrincipal().toString();

    IamAccount account = accountRepository.findByUsername(username)
        .orElseThrow(() -> new IamAccountException("Account for " + username + " not found"));

    return UserRef.builder()
        .id(account.getUuid())
        .userName(account.getUsername())
        .ref(scimResourceLocationProvider.userLocation(account.getUuid()))
        .build();
  }

  private IdTokenRef buildIdTokenRef(OAuth2AccessTokenEntity idToken) {

    if (idToken == null) {
      return null;
    }

    return IdTokenRef.builder()
        .id(idToken.getId())
        .ref(tokensResourceLocationProvider.accessTokenLocation(idToken.getId()))
        .build();
  }
}

package it.infn.mw.iam.authn;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;

import it.infn.mw.iam.persistence.model.IamAccount;

public abstract class AbstractExternalAuthenticationToken<T extends Serializable>
    extends ExpiringUsernameAuthenticationToken {

  /**
   * 
   */
  private static final long serialVersionUID = 3728054624371667370L;

  final T wrappedAuthentication;

  public AbstractExternalAuthenticationToken(T authn, Object principal, Object credentials) {
    super(principal, credentials);
    this.wrappedAuthentication = authn;

  }

  public AbstractExternalAuthenticationToken(T authn, Date tokenExpiration, Object principal,
      Object credentials, Collection<? extends GrantedAuthority> authorities) {
    super(tokenExpiration, principal, credentials, authorities);
    this.wrappedAuthentication = authn;
  }

  public T getExternalAuthentication() {
    return wrappedAuthentication;
  }

  public abstract Map<String, String> buildAuthnInfoMap(ExternalAuthenticationInfoBuilder visitor);

  public abstract void linkToIamAccount(ExternalAccountLinker visitor, IamAccount account);

  public abstract ExternalAuthenticationRegistrationInfo toExernalAuthenticationRegistrationInfo();

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((wrappedAuthentication == null) ? 0 : wrappedAuthentication.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractExternalAuthenticationToken other = (AbstractExternalAuthenticationToken) obj;
    if (wrappedAuthentication == null) {
      if (other.wrappedAuthentication != null)
        return false;
    } else if (!wrappedAuthentication.equals(other.wrappedAuthentication))
      return false;
    return true;
  }
}

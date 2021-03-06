package it.infn.mw.iam.authn.saml.util;

import org.opensaml.saml2.core.NameID;
import org.springframework.security.saml.SAMLCredential;

import com.google.common.base.Verify;

import it.infn.mw.iam.persistence.model.IamSamlId;

public class NameIdUserIdentifierResolver extends AbstractSamlUserIdentifierResolver{

  public static final String NAMEID_RESOLVER = "nameID";
  
  public NameIdUserIdentifierResolver() {
    super(NAMEID_RESOLVER);
  }

  @Override
  public SamlUserIdentifierResolutionResult resolveSamlUserIdentifier(
      SAMLCredential samlCredential) {
   
    Verify.verifyNotNull(samlCredential);
    
    if (samlCredential.getNameID() != null) {
      NameID nameId = samlCredential.getNameID();

      IamSamlId samlId = new IamSamlId();
      samlId.setAttributeId(nameId.getFormat());
      samlId.setUserId(nameId.getValue());
      samlId.setIdpId(samlCredential.getRemoteEntityID());

      return SamlUserIdentifierResolutionResult.resolutionSuccess(samlId);

    }
    
    return SamlUserIdentifierResolutionResult.resolutionFailure("NameID resolution failure");
  }

}

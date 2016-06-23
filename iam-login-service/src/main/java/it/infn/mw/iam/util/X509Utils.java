package it.infn.mw.iam.util;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import it.infn.mw.iam.api.scim.exception.ScimValidationException;

public class X509Utils {

  private X509Utils() {}

  public static X509Certificate getX509CertificateFromString(String certValue)
      throws ScimValidationException {

    byte[] base64decoded = null;
    try {

      base64decoded = Base64.getDecoder().decode(certValue);

    } catch (IllegalArgumentException iae) {

      throw new ScimValidationException(
          "Error in conversion from String to x509 certificate: Not valid Base64 scheme");
    }

    X509Certificate cert = null;

    try {

      cert = (X509Certificate) CertificateFactory.getInstance("X.509")
        .generateCertificate(new ByteArrayInputStream(base64decoded));

    } catch (CertificateException ce) {

      throw new ScimValidationException(
          "Error in conversion from String to x509 certificate: the base64 encoded string is not a valid certificate");
    }

    return cert;
  }
  
  public static String getCertificateSubject(X509Certificate cert) {
    
    return X500NameUtils.getReadableForm(cert.getIssuerX500Principal());
  }
  
public static String getCertificateSubject(String certValueAsString) {
    
    return getCertificateSubject(getX509CertificateFromString(certValueAsString));
  }

}

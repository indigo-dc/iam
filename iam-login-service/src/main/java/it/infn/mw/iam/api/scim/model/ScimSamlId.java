package it.infn.mw.iam.api.scim.model;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.infn.mw.iam.authn.saml.util.Saml2Attribute;

@JsonInclude(Include.NON_EMPTY)
public class ScimSamlId {

  @NotBlank
  @Length(max = 256)
  private final String idpId;

  @NotBlank
  @Length(max = 256)
  private final String userId;

  @NotBlank
  @Length(max = 256)
  private final String attributeId;

  @JsonCreator
  private ScimSamlId(@JsonProperty("idpId") String idpId, @JsonProperty("userId") String userId,
      @JsonProperty("attributeId") String attributeId) {

    this.userId = userId;
    this.idpId = idpId;
    this.attributeId = attributeId;
  }

  private ScimSamlId(Builder b) {

    this.idpId = b.idpId;
    this.userId = b.userId;
    this.attributeId = b.attributeId;
  }

  public String getUserId() {

    return userId;
  }

  public String getIdpId() {

    return idpId;
  }

  public String getAttributeId() {
    return attributeId;
  }

  public static Builder builder() {

    return new Builder();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((idpId == null) ? 0 : idpId.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ScimSamlId other = (ScimSamlId) obj;
    if (idpId == null) {
      if (other.idpId != null)
        return false;
    } else if (!idpId.equals(other.idpId))
      return false;
    if (userId == null) {
      if (other.userId != null)
        return false;
    } else if (!userId.equals(other.userId))
      return false;
    return true;
  }

  public static class Builder {

    private String idpId;
    private String userId;
    private String attributeId = Saml2Attribute.EPUID.getAttributeName();

    public Builder idpId(String idpId) {

      this.idpId = idpId;
      return this;
    }

    public Builder userId(String userId) {

      this.userId = userId;
      return this;
    }

    public Builder attributeId(String attributeId) {
      this.attributeId = attributeId;
      return this;
    }

    public ScimSamlId build() {

      return new ScimSamlId(this);
    }
  }
}

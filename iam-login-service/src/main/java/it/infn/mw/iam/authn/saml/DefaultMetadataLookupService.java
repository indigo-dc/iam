package it.infn.mw.iam.authn.saml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.samlext.saml2mdui.Logo;
import org.opensaml.samlext.saml2mdui.UIInfo;
import org.opensaml.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.saml.metadata.MetadataManager;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import it.infn.mw.iam.authn.saml.model.IdpDescription;

@Component
@Profile("saml")
public class DefaultMetadataLookupService implements MetadataLookupService {

  private static final int MAX_RESULTS = 20;
  private static final Logger LOG = LoggerFactory.getLogger(DefaultMetadataLookupService.class);

  Set<IdpDescription> descriptions = new HashSet<>();
  ReadWriteLock lock = new ReentrantReadWriteLock(true);

  MetadataManager metadataManager;

  @Autowired
  public DefaultMetadataLookupService(MetadataManager manager) {
    this.metadataManager = manager;
    refreshMetadata();
  }

  private void initializeMetadataSet() throws MetadataProviderException {

    LOG.info("Initializing IdP descriptor list from metadata");

    Set<IdpDescription> newDescriptions = new HashSet<>();

    for (String idpName : metadataManager.getIDPEntityNames()) {

      IdpDescription idpDescription =
          descriptionFromMetadata(metadataManager.getEntityDescriptor(idpName));

      LOG.debug("Adding IdP description: {}", idpDescription);
      newDescriptions.add(idpDescription);
    }

    try {
      lock.writeLock().lock();
      descriptions = newDescriptions;
    } finally {
      lock.writeLock().unlock();
    }

  }

  private IdpDescription descriptionFromMetadata(EntityDescriptor descriptor) {
    IdpDescription result = new IdpDescription();
    result.setEntityId(descriptor.getEntityID());

    IDPSSODescriptor idpDesc = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
    if (idpDesc != null && idpDesc.getExtensions() != null) {

      for (final XMLObject object : idpDesc.getExtensions()
        .getUnknownXMLObjects(UIInfo.DEFAULT_ELEMENT_NAME)) {
        if (object instanceof UIInfo) {
          UIInfo uiInfo = (UIInfo) object;

          if (!uiInfo.getDisplayNames().isEmpty()) {
            result.setOrganizationName(uiInfo.getDisplayNames().get(0).getName().getLocalString());
          }

          if (!uiInfo.getLogos().isEmpty()) {
            uiInfo.getLogos().stream().min(Comparator.comparing(Logo::getHeight)).ifPresent(
                l -> result.setImageUrl(l.getURL()));
          }
        }
      }
    }

    if (Strings.isNullOrEmpty(result.getOrganizationName())) {
      result.setOrganizationName(result.getEntityId());
    }

    return result;
  }


  private Optional<List<IdpDescription>> lookupByEntityId(String text) {
    // Try entityId match
    try {

      EntityDescriptor entityDescriptor = metadataManager.getEntityDescriptor(text);
      if (entityDescriptor != null) {
        return Optional.of(ImmutableList.of(descriptionFromMetadata(entityDescriptor)));
      }

    } catch (MetadataProviderException e) {
      throw new SamlMetadataError(e.getMessage(), e);
    }

    return Optional.empty();
  }

  @Override
  public List<IdpDescription> lookupIdp(String text) {

    List<IdpDescription> result = new ArrayList<>();

    lookupByEntityId(text).ifPresent(result::addAll);

    if (!result.isEmpty()) {
      return result;
    }

    try {
      lock.readLock().lock();
      return descriptions.stream()
        .filter(p -> p.getOrganizationName().toLowerCase().contains(text.toLowerCase()))
        .limit(MAX_RESULTS)
        .collect(Collectors.toList());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public List<IdpDescription> listIdps() {
    Set<String> idpNames = metadataManager.getIDPEntityNames();

    List<IdpDescription> results = new ArrayList<>();

    for (String idpName : idpNames) {
      try {
        IdpDescription desc = descriptionFromMetadata(metadataManager.getEntityDescriptor(idpName));

        if (!Strings.isNullOrEmpty(desc.getOrganizationName())) {
          results.add(desc);
        }

      } catch (MetadataProviderException e) {
        LOG.warn("Error accessing metadata for entity: {}", idpName, e);
        continue;
      }

    }

    return results;

  }

  @Override
  public void refreshMetadata() {
    try {
      initializeMetadataSet();
    } catch (MetadataProviderException e) {
      throw new SamlMetadataError(e);
    }
  }

}

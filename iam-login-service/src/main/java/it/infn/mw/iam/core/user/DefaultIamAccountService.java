package it.infn.mw.iam.core.user;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import it.infn.mw.iam.audit.events.account.AccountCreatedEvent;
import it.infn.mw.iam.audit.events.account.AccountRemovedEvent;
import it.infn.mw.iam.core.user.exception.CredentialAlreadyBoundException;
import it.infn.mw.iam.core.user.exception.InvalidCredentialException;
import it.infn.mw.iam.core.user.exception.UserAlreadyExistsException;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.model.IamAuthority;
import it.infn.mw.iam.persistence.model.IamOidcId;
import it.infn.mw.iam.persistence.model.IamSamlId;
import it.infn.mw.iam.persistence.model.IamSshKey;
import it.infn.mw.iam.persistence.model.IamX509Certificate;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.persistence.repository.IamAuthoritiesRepository;

@Service
@Transactional
public class DefaultIamAccountService implements IamAccountService {

  private final IamAccountRepository accountRepo;
  private final IamAuthoritiesRepository authoritiesRepo;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public DefaultIamAccountService(IamAccountRepository accountRepo,
      IamAuthoritiesRepository authoritiesRepo, PasswordEncoder passwordEncoder,
      ApplicationEventPublisher eventPublisher) {

    this.accountRepo = accountRepo;
    this.authoritiesRepo = authoritiesRepo;
    this.passwordEncoder = passwordEncoder;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public IamAccount createAccount(IamAccount account) {
    checkNotNull(account, "Cannot create a null account");

    final Date now = new Date();
    final String randomUuid = UUID.randomUUID().toString();

    newAccountSanityChecks(account);

    if (account.getCreationTime() == null) {
      account.setCreationTime(now);
    }

    if (account.getUuid() == null) {
      account.setUuid(randomUuid);
    }

    account.setLastUpdateTime(now);

    account.getUserInfo().setEmailVerified(true);

    if (account.getPassword() == null) {
      account.setPassword(UUID.randomUUID().toString());
    }

    account.setPassword(passwordEncoder.encode(account.getPassword()));

    IamAuthority roleUserAuthority = authoritiesRepo.findByAuthority("ROLE_USER").orElseThrow(
        () -> new IllegalStateException("ROLE_USER not found in database. This is a bug"));

    account.getAuthorities().add(roleUserAuthority);

    // Credentials sanity checks
    newAccountX509CertificatesSanityChecks(account);
    newAccountSshKeysSanityChecks(account);
    newAccountSamlIdsSanityChecks(account);
    newAccountOidcIdsSanityChecks(account);
    
    // Set creation time for certificates
    account.getX509Certificates().forEach(c -> {
      c.setCreationTime(now);
      c.setLastUpdateTime(now);
    });

    accountRepo.save(account);

    eventPublisher.publishEvent(new AccountCreatedEvent(this, account,
        "Account created for user " + account.getUsername()));

    return account;
  }

  @Override
  public IamAccount deleteAccount(IamAccount account) {
    checkNotNull(account, "cannot delete a null account");
    accountRepo.delete(account);

    eventPublisher.publishEvent(new AccountRemovedEvent(this, account,
        "Removed account for user " + account.getUsername()));

    return account;
  }

  private void newAccountOidcIdsSanityChecks(IamAccount account) {
    account.getOidcIds().forEach(this::oidcIdSanityChecks);
  }


  private void newAccountSamlIdsSanityChecks(IamAccount account) {
    account.getSamlIds().forEach(this::samlIdSanityChecks);
  }

  private void newAccountSanityChecks(IamAccount account) {
    checkArgument(!isNullOrEmpty(account.getUsername()), "Null or empty username");
    checkNotNull(account.getUserInfo(), "Null userinfo object");
    checkArgument(!isNullOrEmpty(account.getUserInfo().getEmail()), "Null or empty email");

    accountRepo.findByUsername(account.getUsername()).ifPresent(a -> {
      throw new UserAlreadyExistsException(
          String.format("A user with username '%s' already exists", a.getUsername()));
    });

    accountRepo.findByEmail(account.getUserInfo().getEmail()).ifPresent(a -> {
      throw new UserAlreadyExistsException(String
        .format("A user linked with email '%s' already exists", a.getUserInfo().getEmail()));
    });

  }

  private void newAccountSshKeysSanityChecks(IamAccount account) {

    if (account.hasSshKeys()) {

      account.getSshKeys().forEach(this::sshKeySanityChecks);

      final long count = account.getSshKeys().stream().filter(IamSshKey::isPrimary).count();

      if (count > 1) {
        throw new InvalidCredentialException("Only one SSH key can be marked as primary");
      }

      if (count == 0) {
        account.getSshKeys().stream().findFirst().ifPresent(k -> k.setPrimary(true));
      }
    }
  }

  private void newAccountX509CertificatesSanityChecks(IamAccount account) {

    if (account.hasX509Certificates()) {

      account.getX509Certificates().forEach(this::x509CertificateSanityCheck);

      final long count =
          account.getX509Certificates().stream().filter(IamX509Certificate::isPrimary).count();

      if (count > 1) {
        throw new InvalidCredentialException("Only one X.509 certificate can be marked as primary");
      }

      if (count == 0) {
        account.getX509Certificates().stream().findFirst().ifPresent(c -> c.setPrimary(true));
      }
    }

  }

  private void oidcIdSanityChecks(IamOidcId oidcId) {
    checkNotNull(oidcId, "null oidc id");
    checkArgument(!isNullOrEmpty(oidcId.getIssuer()), "null or empty oidc id issuer");
    checkArgument(!isNullOrEmpty(oidcId.getSubject()), "null or empty oidc id subject");

    accountRepo.findByOidcId(oidcId.getIssuer(), oidcId.getSubject()).ifPresent(account -> {

      throw new CredentialAlreadyBoundException(String.format(
          "OIDC id '%s,%s' is already bound to a user", oidcId.getIssuer(), oidcId.getSubject()));
    });
  }

  private void samlIdSanityChecks(IamSamlId samlId) {

    checkNotNull(samlId, "null saml id");

    checkArgument(!isNullOrEmpty(samlId.getIdpId()), "null or empty idpId");
    checkArgument(!isNullOrEmpty(samlId.getUserId()), "null or empty userId");
    checkArgument(!isNullOrEmpty(samlId.getAttributeId()), "null or empty attributeId");

    accountRepo.findBySamlId(samlId).ifPresent(account -> {
      throw new CredentialAlreadyBoundException(
          String.format("SAML id '%s,%s,%s' already bound to a user", samlId.getIdpId(),
              samlId.getAttributeId(), samlId.getUserId()));
    });
  }

  private void sshKeySanityChecks(IamSshKey sshKey) {

    checkNotNull(sshKey, "null ssh key");
    checkArgument(!isNullOrEmpty(sshKey.getValue()), "null or empty ssh key value");

    accountRepo.findBySshKeyValue(sshKey.getValue()).ifPresent(account -> {
      throw new CredentialAlreadyBoundException(
          String.format("SSH key '%s' already bound to a user", sshKey.getValue()));
    });
  }

  private void x509CertificateSanityCheck(IamX509Certificate cert) {
    checkNotNull(cert, "null X.509 certificate");
    checkArgument(!isNullOrEmpty(cert.getSubjectDn()),
        "null or empty X.509 certificate subject DN");
    checkArgument(!isNullOrEmpty(cert.getIssuerDn()), "null or empty X.509 certificate issuer DN");
    checkArgument(!isNullOrEmpty(cert.getLabel()), "null or empty X.509 certificate label");

    accountRepo.findByCertificateSubject(cert.getSubjectDn()).ifPresent(c -> {
      throw new CredentialAlreadyBoundException(String
        .format("X509 certificate with subject '%s' is already bound to another user", cert.getSubjectDn()));
    });
  }

  @Override
  public List<IamAccount> deleteInactiveProvisionedUsersSinceTime(Date timestamp) {
    checkNotNull(timestamp, "null timestamp");

    List<IamAccount> accounts =
        accountRepo.findProvisionedAccountsWithLastLoginTimeBeforeTimestamp(timestamp);

    accounts.forEach(this::deleteAccount);

    return accounts;
  }
}

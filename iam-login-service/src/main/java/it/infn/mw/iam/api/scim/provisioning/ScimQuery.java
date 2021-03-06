package it.infn.mw.iam.api.scim.provisioning;

import it.infn.mw.iam.api.scim.provisioning.paging.ScimPageRequest;

public interface ScimQuery extends ScimPageRequest {

  public enum SortOrder {
    ascending, descending;
  }

  String getFilter();

  SortOrder getSortOder();

}

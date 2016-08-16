/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.List;

public class AutoGeneratedKeyAttributes {

  /**
   * Indicates when to make auto-generated keys available for retrieval.
   */
  @Parameter
  @Optional(defaultValue = "false")
  private boolean autoGeneratedKeys = false;

  /**
   * List of column indexes that indicates which auto-generated keys to make
   * available for retrieval.
   */
  @Parameter
  @Optional
  private List<String> autoGeneratedKeysColumnIndexes;

  /**
   * List of column names that indicates which auto-generated keys should be made
   * available for retrieval.
   */
  @Parameter
  @Optional
  private List<String> autoGeneratedKeysColumnNames;

  public boolean isAutoGeneratedKeys() {
    return autoGeneratedKeys;
  }

  public List<String> getAutoGeneratedKeysColumnIndexes() {
    return autoGeneratedKeysColumnIndexes;
  }

  public List<String> getAutoGeneratedKeysColumnNames() {
    return autoGeneratedKeysColumnNames;
  }
}

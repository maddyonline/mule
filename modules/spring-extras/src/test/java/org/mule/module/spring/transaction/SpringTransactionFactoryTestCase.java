/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class SpringTransactionFactoryTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testCommit() throws Exception
    {
        Mock mockPTM = new Mock(PlatformTransactionManager.class);
        Mock mockTS = new Mock(TransactionStatus.class);
        mockPTM.expectAndReturn("getTransaction", C.same(null), mockTS.proxy());
        mockPTM.expect("commit", C.same(mockTS.proxy()));

        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager((PlatformTransactionManager)mockPTM.proxy());

        Transaction tx = factory.beginTransaction(muleContext);
//        TransactionCoordination.getInstance().bindTransaction(tx);
        tx.commit();
    }

    @Test
    public void testRollback() throws Exception
    {
        Mock mockPTM = new Mock(PlatformTransactionManager.class);
        Mock mockTS = new Mock(TransactionStatus.class);
        mockPTM.expectAndReturn("getTransaction", C.same(null), mockTS.proxy());
        mockPTM.expect("rollback", C.same(mockTS.proxy()));
        mockTS.expect("setRollbackOnly");

        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager((PlatformTransactionManager)mockPTM.proxy());

        Transaction tx = factory.beginTransaction(muleContext);
//        TransactionCoordination.getInstance().bindTransaction(tx);
        tx.rollback();
    }

}

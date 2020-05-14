// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.log;

import com.google.common.testing.ForwardingWrapperTester;
import org.junit.Test;

public class ForwardingLogManagerTest {

  @Test
  public void test() {
    new ForwardingWrapperTester()
        .testForwarding(
            LogManager.class,
            pInput ->
                new ForwardingLogManager() {

                  @Override
                  protected LogManager delegate() {
                    return pInput;
                  }

                  // following makes only sense in test

                  @Override
                  public LogManager withComponentName(String pName) {
                    return pInput.withComponentName(pName);
                  }

                  @Override
                  public String toString() {
                    return pInput.toString();
                  }
                });
  }
}

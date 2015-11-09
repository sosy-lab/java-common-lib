/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common.log;

import com.google.common.base.Function;
import com.google.common.testing.ForwardingWrapperTester;

import org.junit.Test;

public class ForwardingLogManagerTest {

  @Test
  public void test() {
    new ForwardingWrapperTester()
        .testForwarding(
            LogManager.class,
            new Function<LogManager, LogManager>() {
              @Override
              public LogManager apply(final LogManager pInput) {
                return new ForwardingLogManager() {

                  @Override
                  protected LogManager delegate() {
                    return pInput;
                  }

                  // following makes only sense in test

                  @Override
                  public LogManager withComponentName(String pName) {
                    return delegate().withComponentName(pName);
                  }

                  @Override
                  public String toString() {
                    return delegate().toString();
                  }
                };
              }
            });
  }
}
